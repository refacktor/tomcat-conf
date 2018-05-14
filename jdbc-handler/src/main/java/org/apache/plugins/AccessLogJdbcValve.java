package org.apache.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;

/**
 * Minimalistic JDBC log handler plugin (Valve) for Tomcat Access Logs. 
 * Serves same purpose as org.apache.catalina.valves.JDBCAccessLogValve.
 * 
 */
public class AccessLogJdbcValve extends AccessLogValve {

	private final boolean DEBUG = false;

	private String dbUrl = null;
	private String driver;
	private String user;
	private String password;

	private String sqlStatement = "insert into log_access (server_ts,remote_ip,local_ip,method,url,query_string,protocol,http_status,bytes_sent,referer,user_agent,time_elapsed,session_id,user_id,agent_proxy,agent_id,time_to_first_byte,thread_name) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private String pattern = "%{y-M-d H:m:s.S}t%a%A%m%U%q%H%s%B%{Referer}i%{User-Agent}i%D%S%{user_id}s%{agent_proxy}s%{agend_device_id}c%F%I";
	
	public AccessLogJdbcValve() {
		try {

			Properties p = new Properties();
			String path = System.getProperty("ctc.config.path");
			URL propertiesUrl = new URL(path + "/tomcat_db_logging.properties");
			InputStream is = propertiesUrl.openStream();
			p.load(is);
			is.close();

			this.dbUrl = p.getProperty("database.url");
			this.driver = p.getProperty("database.driver.classname");
			this.user = p.getProperty("database.username");
			this.password = p.getProperty("database.password");

			Class.forName(driver);

			try(Connection test = DriverManager.getConnection(dbUrl, user, password)) {
				//empty;
			}

			System.out.println(new Date() + " " + this.getClass().getName() + " Connected to database " + dbUrl);

		} catch (IOException | SQLException | ClassNotFoundException e) {
			System.err.println(new Date() + " " + this.getClass().getName() + " FAILED TO CONNECT TO DATABASE " + dbUrl);
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void log(String nothing) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void log(Request request, Response response, long time) {
		if (!getState().isAvailable() || !getEnabled() || logElements == null
				|| condition != null && null != request.getRequest().getAttribute(condition)) {
			return;
		}

		Date date = new Date();

		int retries = 3;

		while (--retries >= 0) {

			StringBuilder debug = new StringBuilder();
			int n = 0;

			try(Connection connection = DriverManager.getConnection(dbUrl, user, password);
				PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
				
				for (int i = 0; i < logElements.length; i++) {

					if (logElements[i] instanceof StringElement)
						// ignore extraneous elements introduced by the pattern
						// parser
						continue;

					StringBuilder result = new StringBuilder(128);
					logElements[i].addElement(result, date, request, response, time);

					String value = result.toString();
					if ("-".equals(value)) {
						value = null;
					}
					
					++n;

					statement.setString(n, value);
					debug.append("Called setString(" + n + ", [" + value + "])\n");					
				}
				if (statement.executeUpdate() != 1) {
					throw new SQLException("not inserted 1 row");
				}
				return;

			} catch (SQLException e) {

				System.err.println(new Date() + " " + this.getClass().getName()
						+ ": Failed to log to database! Will retry another " + retries + " times. Error: "
						+ e.toString() + " debug={" + debug.toString() + "}");
				
				e.printStackTrace(System.err);
			}
		}

	}

	@Override
	protected synchronized void open() {
		// do nothing
	}

	@Override
	public void setPattern(String ignore) {
		assert (ignore.equals("default"));
		if (DEBUG)
			System.err.println(new Date() + ": Pattern = " + pattern);
		super.setPattern(pattern);
		if (DEBUG)
			for (int i = 0; i < logElements.length; i++) {
				System.err.printf("[%d] = %s\n", i, logElements[i].getClass().getName());
			}
	}
}
