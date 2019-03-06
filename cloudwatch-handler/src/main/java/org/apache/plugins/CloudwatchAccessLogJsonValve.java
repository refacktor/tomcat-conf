package org.apache.plugins;

import java.io.CharArrayWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CloudwatchAccessLogJsonValve extends AccessLogValve {

	protected CloudwatchClient client;
	
	public CloudwatchAccessLogJsonValve() {
		this(CloudwatchClient.getInstance());
	}

	public CloudwatchAccessLogJsonValve(CloudwatchClient cloudwatchHandler) {
		this.client = cloudwatchHandler;
	}

	private String fields[];

	private String pattern = null;
	
	@Override
	public void log(CharArrayWriter nothing) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void log(Request request, Response response, long time) {
		if (!getState().isAvailable() || !getEnabled() || logElements == null
				|| condition != null && null != request.getRequest()
						.getAttribute(condition)) {
			return;
		}

		Date date = timestamp();
		int n = 0;
		ObjectNode json = new ObjectNode(JsonNodeFactory.instance);

		for (int i = 0; i < logElements.length; i++) {

			if (logElements[i] instanceof StringElement)
				// ignore extraneous elements introduced by the pattern
				// parser
				continue;

			CharArrayWriter result = new CharArrayWriter(128);
			logElements[i].addElement(result, date, request, response, time);

			String value = result.toString();
			if ("-".equals(value)) {
				value = null;
			}

			json.put(fields[n], value);

			++n;
		}

		client.publish(new JsonLogRecord(json));

	}

	protected Date timestamp() {
		return new Date();
	}

	@Override
	protected synchronized void open() {
		// do nothing
	}

	@Override
	public void setPattern(String patternParm) {
		String defaultPattern =
				"server_ts=%{y-MM-dd HH:mm:ss.SSS z}t,remote_ip=%a,local_ip=%A,method=%m,url=%U,query_string=%q,protocol=%H,http_status=%s,bytes_sent=%B,referer=%{Referer}i,user_agent=%{User-Agent}i,time_elapsed=%D,session_id=%S,user_id=%{user_id}s,agent_proxy=%{agent_proxy}s,agent_id=%{agent_device_id}c,time_to_first_byte=%F,thread_name=%I,host=%{Host}i";

		List<String> pairs = Arrays.asList((patternParm.equals("default") ? defaultPattern : patternParm).split(","));
		
		this.pattern = pairs.stream().map(s -> s.split("=")[1]).collect(Collectors.joining(""));
		this.fields =  pairs.stream().map(s -> s.split("=")[0]).toArray(String[]::new);

		super.setPattern(pattern);
	}

}
