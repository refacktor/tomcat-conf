package org.apache.plugins;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class JsonLogRecord extends LogRecord {

	private ObjectNode json;
	
	private static final ObjectMapper om = new ObjectMapper();

	public JsonLogRecord(ObjectNode json) {
		super(Level.ALL, null);
		this.json = json;
	}

	public final String getMessage() {
		try {
			return om.writeValueAsString(json);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}