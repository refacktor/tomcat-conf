package org.apache.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

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

	public JdbcHandler() {
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
			System.out.println("Don't forget to create table: create table if not exists log_raw_xml(uts TIMESTAMP, xml LONGTEXT)");

		} catch (IOException | SQLException | ClassNotFoundException e) {
			System.err.println("something wrong with configuration properties");
			e.printStackTrace(System.err);
			System.exit(-1);
		}
	}

	private void connect() throws SQLException {
		connection = DriverManager.getConnection(dbUrl, user, password);
		pStmtInsert = connection.prepareStatement("INSERT INTO log_raw_xml VALUES (NOW(), ?)");
	}

	@Override
	public void publish(LogRecord record) {

		if (getFilter() != null && !getFilter().isLoggable(record))
			return;

		int retries = 3;

		while (retries-- >= 0) {
			try {
				JAXBContext jc = JAXBContext.newInstance(LogRecord.class);
				JAXBElement<LogRecord> je = new JAXBElement<>(new QName("log"), LogRecord.class, record);
				Marshaller marshaller = jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				ByteArrayOutputStream xml = new ByteArrayOutputStream();
				marshaller.marshal(je, xml);

				pStmtInsert.setString(1, xml.toString());
				pStmtInsert.executeUpdate();

			} catch (SQLException | JAXBException e) {
				System.err.println("Failed to log to database! Will retry another " + retries + " times.");
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