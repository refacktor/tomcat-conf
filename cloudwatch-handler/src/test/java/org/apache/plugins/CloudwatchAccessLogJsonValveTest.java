package org.apache.plugins;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.LogRecord;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CloudwatchAccessLogJsonValveTest {

	@Test
	public void test() throws JsonProcessingException, IOException {

		CloudwatchClient jdkLogger = Mockito.mock(CloudwatchClient.class);
		CloudwatchAccessLogJsonValve target = Mockito.spy(new CloudwatchAccessLogJsonValve(jdkLogger));
		
		Mockito.when(target.getState()).thenReturn(LifecycleState.STARTED);
		Mockito.when(target.getEnabled()).thenReturn(true);
		
		target.setPattern("default");
		
		Request request = Mockito.mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
		org.apache.coyote.Request coyoteRequest = new org.apache.coyote.Request();
		Mockito.when(request.getCoyoteRequest()).thenReturn(coyoteRequest);
		Mockito.when(request.getProtocol()).thenReturn("httpx");
		
		Response response = Mockito.mock(Response.class, Mockito.RETURNS_DEEP_STUBS); 
		final org.apache.coyote.Response coyoteResponse = new org.apache.coyote.Response();
		coyoteResponse.setCommitted(true);
		Mockito.when(response.getCoyoteResponse()).thenReturn(coyoteResponse);

		List<LogRecord> out = new ArrayList<>();
		
		Mockito.doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return out.add(invocation.getArgumentAt(0, LogRecord.class));
			}
		}).when(target.client).publish(Mockito.any(LogRecord.class));

		Date nowz = Date.from(Instant.parse("2018-01-02T01:02:03Z"));
		Mockito.when(target.timestamp()).thenReturn(nowz);
		
		target.log(request, response, 0);

		Assert.assertEquals(1, out.size());
		JsonLogRecord lr = (JsonLogRecord) out.get(0);
		ObjectMapper om = new ObjectMapper();
		JsonNode readTree = om.readTree(lr.getMessage());
		Assert.assertEquals("httpx", readTree.get("protocol").asText());
		Assert.assertEquals("2018-01-01 17:02:03.000 PST", readTree.get("server_ts").asText());
	}
}
