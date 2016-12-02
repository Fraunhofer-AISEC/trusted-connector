package de.fhg.ids.attestation;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.rat.*;

public class Database {
	
	static java.sql.Connection connection = null;
	static java.sql.PreparedStatement statement = null;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
 
	public Database() {
		makeJDBCConnection(Constants.HOST, Constants.PORT, Constants.DATABASE, Constants.USERNAME, Constants.PASSWORD);
	}

	private void makeJDBCConnection(String host, String port, String db, String user, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			LOG.debug("Sorry, couldn't found JDBC driver. Make sure you have added JDBC Maven Dependency Correctly");
			e.printStackTrace();
			return;
		}
		try {
			connection = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/" + db, user, password);
			if (connection != null) {
				LOG.debug("connection to mysql db successful!");
			} else {
				LOG.debug("Failed to make connection to mysql db!");
			}
		} catch (SQLException e) {
			LOG.debug(e.getMessage());
			return;
		}

	}
	
	public void close() throws SQLException {
		if(statement != null) {
			statement.close();
		}
		if(connection != null) {
			connection.close();
		}
	}
 
	/*
	private static void insertValues(PcrMessage values) {
		try {
			
			String insertQueryStatement = "INSERT  INTO  pcr  VALUES  (?,?)";
			statement = connection.prepareStatement(insertQueryStatement);
			statement.setInt(3, totalEmployee);
			statement.setString(4, webSite);
 
			// execute insert SQL statement
			statement.executeUpdate();
		} catch (
 
		SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static boolean checkValues(PcrMessage values) {
		try {
			String getQueryStatement = "SELECT * FROM pcr";
			statement = connection.prepareStatement(getQueryStatement);
 			ResultSet rs = statement.executeQuery();
 			while (rs.next()) {
				String name = rs.getString("Name");
				String address = rs.getString("Address");
				int employeeCount = rs.getInt("EmployeeCount");
				String website = rs.getString("Website");
 			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	*/
}