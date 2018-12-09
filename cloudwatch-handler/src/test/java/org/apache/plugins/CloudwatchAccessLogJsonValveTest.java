package org.apache.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CloudwatchAccessLogJsonValveTest {

	@Test
	public void test() throws JsonProcessingException, IOException {

		CloudwatchHandler jdkLogger = Mockito.mock(CloudwatchHandler.class);
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
		}).when(target.jdkLogger).publish(Mockito.any(LogRecord.class));
		
		target.log(request, response, 0);

		Assert.assertEquals(out.size(), 1);
		JsonLogRecord lr = (JsonLogRecord) out.get(0);
		ObjectMapper om = new ObjectMapper();
		JsonNode readTree = om.readTree(lr.getMessage());
		Assert.assertEquals(readTree.get("protocol").asText(), "httpx");
	}
}
