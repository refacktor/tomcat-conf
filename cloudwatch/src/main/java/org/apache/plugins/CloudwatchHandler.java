package org.apache.plugins;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;

public class CloudwatchHandler extends Handler {

	private final Boolean DEBUG_MODE = true;

	/**
	 * The queue used to buffer log entries
	 */
	private LinkedBlockingQueue<LogRecord> loggingEventsQueue;

	/**
	 * the AWS Cloudwatch Logs API client
	 */
	private AWSLogsClient awsLogsClient;

	private Formatter formatter = new SimpleFormatter();

	private AtomicReference<String> lastSequenceToken = new AtomicReference<>();

	/**
	 * The AWS Cloudwatch Log group name
	 */
	private String logGroupName;

	/**
	 * The AWS Cloudwatch Log stream name
	 */
	private String logStreamName;

	/**
	 * The queue / buffer size
	 */
	private int queueLength = 1024;

	/**
	 * The maximum number of log entries to send in one go to the AWS Cloudwatch
	 * Log service
	 */
	private int messagesBatchSize = 128;

	private AtomicBoolean cloudwatchAppenderInitialised = new AtomicBoolean(false);

	private ScheduledThreadPoolExecutor exe;

	public CloudwatchHandler() {
		super();
		try {
			logGroupName = "/tomcat/" + InetAddress.getLocalHost().getHostName();
			logStreamName = Instant.now().toString().replace(':', '.');
			init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CloudwatchHandler(Formatter formatter, String logGroupName, String logStreamName) {
		super();
		this.setFormatter(formatter);
		this.setLogGroupName(logGroupName);
		this.setLogStreamName(logStreamName);
		this.flush();
	}

	public void setLogGroupName(String logGroupName) {
		this.logGroupName = logGroupName;
	}

	public void setLogStreamName(String logStreamName) {
		this.logStreamName = logStreamName;
	}

	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}

	public void setMessagesBatchSize(int messagesBatchSize) {
		this.messagesBatchSize = messagesBatchSize;
	}

	public Formatter getFormatter() {
		return formatter;
	}

	public void setFormatter(Formatter formatter) {
		this.formatter = formatter;
	}

	private synchronized void sendMessages() {
		LogRecord polledLoggingEvent;
		List<LogRecord> loggingEvents = new ArrayList<>();
		try {
			while ((polledLoggingEvent = loggingEventsQueue.poll()) != null
					&& loggingEvents.size() <= messagesBatchSize) {
				loggingEvents.add(polledLoggingEvent);
			}
			List<InputLogEvent> inputLogEvents = loggingEvents.stream().map(loggingEvent -> new InputLogEvent()
					.withTimestamp(loggingEvent.getMillis()).withMessage(formatter.format(loggingEvent)))
					.collect(toList());
			if (!loggingEvents.isEmpty()) {
				PutLogEventsRequest putLogEventsRequest = new PutLogEventsRequest(logGroupName, logStreamName,
						inputLogEvents);
				try {
					putLogEventsRequest.setSequenceToken(lastSequenceToken.get());
					PutLogEventsResult result = awsLogsClient.putLogEvents(putLogEventsRequest);
					lastSequenceToken.set(result.getNextSequenceToken());
				} catch (InvalidSequenceTokenException invalidSequenceTokenException) {
					System.err.println("Resetting sequenceToken");
					putLogEventsRequest.setSequenceToken(invalidSequenceTokenException.getExpectedSequenceToken());
					PutLogEventsResult result = awsLogsClient.putLogEvents(putLogEventsRequest);
					lastSequenceToken.set(result.getNextSequenceToken());
					if (DEBUG_MODE) {
						invalidSequenceTokenException.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		}
	}

	private void initCloudwatchDaemon() {
		exe = new ScheduledThreadPoolExecutor(1);
		exe.schedule(() -> {
			if (loggingEventsQueue.size() > 0) {
				sendMessages();
			}
		}, 1, TimeUnit.SECONDS);
	}

	private void initializeCloudwatchResources() {
		DescribeLogGroupsRequest describeLogGroupsRequest = new DescribeLogGroupsRequest();
		describeLogGroupsRequest.setLogGroupNamePrefix(logGroupName);
		Optional<LogGroup> logGroupOptional = awsLogsClient.describeLogGroups(describeLogGroupsRequest).getLogGroups()
				.stream().filter(logGroup -> logGroup.getLogGroupName().equals(logGroupName)).findFirst();
		if (!logGroupOptional.isPresent()) {
			CreateLogGroupRequest createLogGroupRequest = new CreateLogGroupRequest().withLogGroupName(logGroupName);
			awsLogsClient.createLogGroup(createLogGroupRequest);
		}
		DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest()
				.withLogGroupName(logGroupName).withLogStreamNamePrefix(logStreamName);
		Optional<LogStream> logStreamOptional = awsLogsClient.describeLogStreams(describeLogStreamsRequest)
				.getLogStreams().stream().filter(logStream -> logStream.getLogStreamName().equals(logStreamName))
				.findFirst();
		if (!logStreamOptional.isPresent()) {
			System.out.println("About to create LogStream: " + logStreamName + "in LogGroup: " + logGroupName);
			CreateLogStreamRequest createLogStreamRequest = new CreateLogStreamRequest().withLogGroupName(logGroupName)
					.withLogStreamName(logStreamName);
			awsLogsClient.createLogStream(createLogStreamRequest);
		}
	}

	private boolean isBlank(String string) {
		return null == string || string.trim().length() == 0;
	}

	@Override
	public void publish(LogRecord record) {
		if (cloudwatchAppenderInitialised.get()) {
			loggingEventsQueue.add(record);
		} else {
			throw new IllegalStateException("Not Initialized");
		}
	}

	public synchronized void init() throws IOException {
		if (isBlank(logGroupName) || isBlank(logStreamName)) {
			System.out.println("Could not initialise CloudwatchAppender because either or both LogGroupName("
					+ logGroupName + ") and LogStreamName(" + logStreamName + ") are null or empty");
			this.close();
		} else {
			initializeClient();
			loggingEventsQueue = new LinkedBlockingQueue<>(queueLength);
			try {
				initializeCloudwatchResources();
				initCloudwatchDaemon();
				cloudwatchAppenderInitialised.set(true);
			} catch (Exception e) {
				System.err.println("Could not initialise Cloudwatch Logs for LogGroupName: " + logGroupName
						+ " and LogStreamName: " + logStreamName + ": " + e.toString());
				throw e;
			}
			System.err.println("Initialized CloudwatchAppender with LogGroupName(" + logGroupName
					+ ") and LogStreamName(" + logStreamName + ")");
		}
	}

	public void initializeClient() throws IOException {
		File cliCreds = new File(System.getenv("HOME") + "/.aws/config");
		if(cliCreds.exists()) {
			System.out.println("Reading credentials from " + cliCreds.getAbsolutePath());
			Properties p = new Properties();
			try(FileInputStream fis = new FileInputStream(cliCreds)) {
				p.load(fis);
			}
			this.awsLogsClient = new AWSLogsClient(new AWSCredentialsProvider() {
				@Override
				public void refresh() {
				}
				@Override
				public AWSCredentials getCredentials() {
					return new AWSCredentials() {
						@Override
						public String getAWSSecretKey() {
							return p.getProperty("aws_secret_access_key");
						}
						
						@Override
						public String getAWSAccessKeyId() {
							return p.getProperty("aws_access_key_id");
						}
					};
				}
			});
			
			this.awsLogsClient.setRegion(Region.getRegion(Regions.fromName(p.getProperty("region"))));
		}
		else {
			System.out.println("Reading AWS credentials from environment");
			this.awsLogsClient = new AWSLogsClient(new EnvironmentVariableCredentialsProvider());
			this.awsLogsClient.setRegion(Regions.getCurrentRegion());
		}
	}

	@Override
	public synchronized void close() throws SecurityException {
		exe.shutdown();
		try {
			exe.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
		flush();
	}

	@Override
	public void flush() {
		sendMessages();
	}
}
