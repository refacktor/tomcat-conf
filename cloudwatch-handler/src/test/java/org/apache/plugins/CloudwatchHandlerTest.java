package org.apache.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.Assert;

public class CloudwatchHandlerTest {

	Formatter formatter = new Formatter() {
		@Override
		public String format(LogRecord record) {
			return "FORMATTED:" + record.getMessage();
		}
	};

	CloudwatchHandler ch
			= new CloudwatchHandler(formatter, "logGroupName", "logStreamName");

	List<PutLogEventsRequest> requests = new ArrayList<>();

	PutLogEventsResult pler = new PutLogEventsResult();

	@BeforeClass
	public void init() {
		ch.awsLogsClient = Mockito.mock(AWSLogsClient.class);
		ch.cloudwatchAppenderInitialised.set(true);
		pler.setNextSequenceToken("1111");
		ch.initCloudwatchDaemon();
		
		Mockito.when(ch.awsLogsClient
				.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
				.thenAnswer(new Answer<PutLogEventsResult>() {

					@Override
					public PutLogEventsResult answer(
							InvocationOnMock invocation) throws Throwable {
						Object[] args = invocation.getArguments();
						requests.add((PutLogEventsRequest) args[0]);
						return pler;
					}
				});
	}
	
	@Test
	public void basicTest() {
		requests.clear();
		final LogRecord lr = new LogRecord(Level.INFO, "test log");
		ch.publish(lr);
		ch.flush();
		Assert.assertEquals(1, requests.size());
		PutLogEventsRequest req = requests.get(0);
		final InputLogEvent inputLogEvent = req.getLogEvents().get(0);
		Assert.assertEquals(formatter.format(lr), inputLogEvent.getMessage());
	}
	
	@Test
	public void jsonTest() {
		requests.clear();
		ObjectNode on = new ObjectNode(JsonNodeFactory.instance);
		on.put("hello", "world");
		final LogRecord lr = new JsonLogRecord(on);
				
		ch.publish(lr);
		ch.flush();
		Assert.assertEquals(1, requests.size());
		PutLogEventsRequest req = requests.get(0);
		final InputLogEvent inputLogEvent = req.getLogEvents().get(0);
		Assert.assertEquals("{\"hello\":\"world\"}", inputLogEvent.getMessage());
	}

	@AfterClass
	public void cleanup() {
		ch.close();
	}
}
