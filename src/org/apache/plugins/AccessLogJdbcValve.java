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
	private PreparedStatement statement;
	private String sqlStatement = "insert into log_access (server_ts,remote_ip,local_ip,method,url,query_string,protocol,http_status,bytes_sent,referer,user_agent,req_time,session_id,user_id,agent_proxy,rsp_time,thread_name) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
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
			
			System.out.println(new Date() + " " + this.getClass().getName() + " Connected to database " + dbUrl);

		} catch (IOException | SQLException | ClassNotFoundException e) {
			System.err.println("something wrong with configuration properties");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	private void connect() throws SQLException {
		connection = DriverManager.getConnection(dbUrl, user, password);
		statement = connection.prepareStatement(sqlStatement);
	}

	@Override
	public void log(String nothing) {
		throw new UnsupportedOperationException();
	}
	
    @Override
    public void log(Request request, Response response, long time) {
        if (!getState().isAvailable() || !getEnabled() ||
                logElements == null || condition != null
                && null != request.getRequest().getAttribute(condition)) {
            return;
        }
        
        Date date = new Date();

		int retries = 3;

		while (--retries >= 0) {

			try {
				
				for (int i = 0; i < logElements.length; i++) {

					if(logElements[i] instanceof StringElement) 
						throw new IllegalArgumentException();

					StringBuilder result = new StringBuilder(128);
					logElements[i].addElement(result, date, request, response, time);
					
					statement.setString(i+1, result.toString());
				}
				if(statement.executeUpdate() != 1) {
					throw new SQLException("not inserted 1 row");
				}
				return;
				
			} catch (SQLException e) {
				
				System.err.println("Failed to log to database! Will retry another " + retries + " times. Error: " + e.toString());
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
    
    public void setStatement(String statement) {
    	this.sqlStatement = statement;
    }

}
