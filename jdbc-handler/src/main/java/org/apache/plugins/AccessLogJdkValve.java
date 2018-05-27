package org.apache.plugins;

import java.io.CharArrayWriter;
import java.util.logging.Logger;

import org.apache.catalina.valves.AccessLogValve;

public class AccessLogJdkValve extends AccessLogValve {

	private static Logger jdkLogger = Logger.getLogger(AccessLogJdkValve.class.getName());
	
	@Override
	public void log(CharArrayWriter msg) {
		jdkLogger.info(msg.toString());
	}
	
	@Override
	protected synchronized void open() {
		// do nothing
	}
}
