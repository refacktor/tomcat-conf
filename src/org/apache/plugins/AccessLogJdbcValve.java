package org.apache.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.catalina.valves.AccessLogValve;

/**
 * Minimalistic JDBC log handler plugin (Valve) for Tomcat Access Logs.
 * Uses standard message formatter to generate SQL, without prepared statements, 
 * which is slower but simpler and more flexible than org.apache.catalina.valves.JDBCAccessLogValve.
 * 
 */
public class AccessLogJdbcValve extends AccessLogValve {

	private String dbUrl;
	private String driver;
	private String user;
	private String password;

	private Connection connection;
	
	public AccessLogJdbcValve() {
		try {
			
			Properties p = new Properties();
			String path = System.getProperty("ctc.config.path");
			URL propertiesUrl = new URL(path + "/database.properties");
			InputStream is = propertiesUrl.openStream();
			p.load(is);
			is.close();

			dbUrl = p.getProperty("database.url");
			driver = p.getProperty("database.driver.classname");
			user = p.getProperty("database.username");
			password = p.getProperty("database.password");

			Class.forName(driver);
			this.connect();
			
			System.out.println("JdbcHandler Connected to database " + dbUrl);

		} catch (IOException | SQLException | ClassNotFoundException e) {
			System.err.println("something wrong with configuration properties");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	private void connect() throws SQLException {
		connection = DriverManager.getConnection(dbUrl, user, password);
	}

	@Override
	public void log(String record) {

		int retries = 3;

		while (--retries >= 0) {
			try(Statement stmt = connection.createStatement()) {
				
				int rc = stmt.executeUpdate(record);
				if(rc != 1) {
					throw new SQLException("Row Count = " + rc);
				}
				return; // don't retry

			} catch (SQLException e) {
				System.err.println("Failed to log to database! Will retry another " + retries + " times. Error: " + e.toString());
				try {
					Thread.sleep(1000);
					this.connect();
					System.err.println("Reconnect OK");
				} catch (InterruptedException | SQLException blah) {
					System.err.println(record);
					System.err.println("Reconnect FAILED");
				}
			}
		}
	}
}