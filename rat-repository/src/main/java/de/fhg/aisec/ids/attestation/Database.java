package de.fhg.aisec.ids.attestation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;

public class Database {
	
	private static PreparedStatement pStatement = null;
	private Logger LOG = LoggerFactory.getLogger(Database.class);
	private Connection connection;
	private Statement statement;
	private String sql;
	private String zero = "0000000000000000000000000000000000000000000000000000000000000000";
 
	public Database() throws SQLException {
		makeJDBCConnection();
		createTables();
		insertDefaultConfiguration();
	}


	private void insertDefaultConfiguration() throws SQLException {
		Pcr[] one = new Pcr[] {
    			Pcr.newBuilder().setNumber(1).setValue(zero).build(), 
    			Pcr.newBuilder().setNumber(2).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(3).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(4).setValue(zero).build()};
		Pcr[] two = new Pcr[] {
    			Pcr.newBuilder().setNumber(1).setValue(zero).build(), 
    			Pcr.newBuilder().setNumber(2).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(3).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(4).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(5).setValue(zero).build(),
    			Pcr.newBuilder().setNumber(6).setValue(zero).build()};
    	this.insertConfiguration("default_one", "BASIC", one);
    	this.insertConfiguration("default_two", "BASIC", two);
	}

	private void makeJDBCConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			LOG.debug("Sorry, couldn't found JDBC driver. Make sure you have added JDBC Maven Dependency Correctly");
			e.printStackTrace();
			return;
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:configuration.db");
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
		if(pStatement != null) {
			pStatement.close();
		}
		if(connection != null) {
			connection.close();
		}
	}
	
	public void createTables() throws SQLException {
		statement = connection.createStatement();
		sql = "DROP TABLE IF EXISTS CONFIG; CREATE TABLE CONFIG ("
				+ "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "'NAME' VARCHAR(255) NULL DEFAULT 'Configuration Name',"
				+ "'TYPE' VARCHAR(255) NULL DEFAULT 'BASIC');"; 
		statement.executeUpdate(sql);
		statement.close();
		
		statement = connection.createStatement();
		sql = "DROP TABLE IF EXISTS PCR; CREATE TABLE PCR ("
				+ "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "'SEQ' INTEGER DEFAULT '0',"
				+ "'VALUE' CHAR(64) NOT NULL DEFAULT '"+this.zero+"',"
				+ "'CID' INTEGER NOT NULL,"
				+ "FOREIGN KEY (CID) REFERENCES 'CONFIG' ('ID'));"; 
		statement.executeUpdate(sql);
		statement.close();
	}
	
	public List<Configuration> getConfigurations() throws SQLException {
		List<Configuration> ll = new LinkedList<Configuration>();
		ResultSet rs = statement.executeQuery("SELECT * FROM CONFIG");
		while (rs.next()) {
			ll.add(new Configuration(rs.getLong("ID"), rs.getString("NAME"), rs.getString("TYPE")));
		}
		return ll;
	}
	
	public void insertConfiguration(String name, String type, Pcr[] values) throws SQLException {
		long key = -1L;
		sql = "INSERT INTO CONFIG (NAME, TYPE) VALUES (?,?)";
		pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		pStatement.setString(1, name);
		pStatement.setString(2, type);
 		pStatement.executeUpdate();
 		pStatement.close();
 		this.insertPcrValues(values, pStatement.getGeneratedKeys().getLong(1));
	}
	
	private void insertPcrValues(Pcr[] values, long key) throws SQLException {
		for(int i = 0; i < values.length; i++) {
			sql = "INSERT INTO PCR (SEQ, VALUE, CID) VALUES  (?,?,?)";
			pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pStatement.setInt(1, values[i].getNumber());
			pStatement.setString(2, values[i].getValue());
			pStatement.setLong(3, key);
	 		pStatement.executeUpdate();
	 		pStatement.close();
		}
	}
}