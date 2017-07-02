package org.apache.plugins;

import java.util.logging.Logger;
import org.apache.catalina.valves.AccessLogValve;

public class AccessLogJdkValve extends AccessLogValve {

	private static Logger jdkLogger = Logger.getLogger(AccessLogJdkValve.class.getName());
	
	@Override
	public void log(String msg) {
		jdkLogger.info(msg);
	}
	
	@Override
	protected synchronized void open() {
		// do nothing
	}
}
