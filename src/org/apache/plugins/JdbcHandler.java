package org.apache.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Minimalistic JDBC log handler plugin for java.util.logging.
 */
public class JdbcHandler extends Handler {

	private String dbUrl;
	private String driver;
	private String user;
	private String password;

	private Connection connection;
	private PreparedStatement pStmtInsert;

	private String hostname;

	public JdbcHandler() {
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();

			Properties p = new Properties();
			String path = System.getProperty("ctc.config.path");
			URL propertiesUrl = new URL(path + "/tomcat_db_logging.properties");
			InputStream is = propertiesUrl.openStream();
			p.load(is);
			is.close();

			dbUrl = p.getProperty("database.url");
			driver = p.getProperty("database.driver.classname");
			user = p.getProperty("database.username");
			password = p.getProperty("database.password");

			Class.forName(driver);
			this.connect();
			
			LogRecord lr = new LogRecord(Level.CONFIG, "JdbcHandler connected successfully");
			lr.setLoggerName(this.getClass().getName());
			lr.setSourceClassName(this.getClass().getName());
			lr.setSourceMethodName("Constructor");
			this.publish(lr);

			System.out.println(new Date() + " " + this.getClass().getName() + " Connected to database " + dbUrl);

		} catch (IOException | SQLException | ClassNotFoundException e) {
			System.err.println("something wrong with configuration properties");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	private void connect() throws SQLException {
		connection = DriverManager.getConnection(dbUrl, user, password);

		pStmtInsert = connection
				.prepareStatement("INSERT INTO log_application(dbTimeStamp,millis,loggerName,message,sequenceNumber,"
						+ "sourceClassName,sourceMethodName,threadID,hostname,level,thrown) VALUES (NOW(),?,?,?,?,?,?,?,?,?,?)");
	}

	@Override
	public void publish(LogRecord record) {

		if (getFilter() != null && !getFilter().isLoggable(record))
			return;

		int retries = 3;

		while (--retries >= 0) {
			try {
				pStmtInsert.setLong(1, record.getMillis());
				pStmtInsert.setString(2, record.getLoggerName());
				pStmtInsert.setString(3, record.getMessage());
				pStmtInsert.setInt(4, (int) record.getSequenceNumber());
				pStmtInsert.setString(5, record.getSourceClassName());
				pStmtInsert.setString(6, record.getSourceMethodName());
				pStmtInsert.setInt(7, (int) record.getThreadID());
				pStmtInsert.setString(8, this.hostname);
				pStmtInsert.setString(9, record.getLevel().getName());

				if (record.getThrown() != null) {
					StringWriter sw = new StringWriter();
					record.getThrown().printStackTrace(new PrintWriter(sw));
					pStmtInsert.setString(10, sw.toString());
				} else {
					pStmtInsert.setString(10, null);
				}

				pStmtInsert.executeUpdate();
				return; // don't retry

			} catch (SQLException e) {
				System.err.println(new Date().toString() + " " + this.getClass().getName());
				System.err.println(
						"Failed to log to database! Will retry another " + retries + " times. Error: " + e.toString());
				try {
					Thread.sleep(1000);
					this.connect();
					System.err.println("Reconnect OK");
				} catch (InterruptedException | SQLException blah) {
					System.err.println("Reconnect FAILED");
				}
			}
		}
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException blah) {
		}
	}

	@Override
	public void flush() {
	}
}
