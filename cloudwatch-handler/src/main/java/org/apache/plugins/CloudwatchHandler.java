package org.apache.plugins;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CloudwatchHandler extends Handler {

	@Override
	public void publish(LogRecord record) {
		CloudwatchClient.getInstance().publish(record);
	}

	@Override
	public void flush() {
		CloudwatchClient.getInstance().flush();
	}

	@Override
	public void close() throws SecurityException {
		CloudwatchClient.getInstance().close();
	}

}
