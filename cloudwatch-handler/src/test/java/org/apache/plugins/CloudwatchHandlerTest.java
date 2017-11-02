//package org.apache.plugins;
//
//import java.util.logging.Level;
//import java.util.logging.LogRecord;
//
//import org.testng.annotations.Test;
//
//public class CloudwatchHandlerTest {
//	
//	@Test
//	public void test() {
//		
//		CloudwatchHandler ch = new CloudwatchHandler();
//		
//		final LogRecord lr = new LogRecord(Level.INFO, "test log");
//		ch.publish(lr);
//		ch.flush();
//		
//		ch.close();
//	}
//	
//}
