package de.fhg.aisec.ids.attestation;

import static java.lang.Math.toIntExact;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sqlite.jdbc4.*;

import com.google.gson.Gson;

public class Database {
	
	private static PreparedStatement pStatement = null;
	private Gson gson = new Gson();
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
		Pcr[] basic = new Pcr[10];
		Pcr[] advanced = new Pcr[24];
		for (int i = 0; i < 10 ; i++) {
			basic[i] = new Pcr(i, zero);
		}
		for (int i = 0; i < 24 ; i++) {
			advanced[i] = new Pcr(i, zero);
		}
		LOG.debug("insertDefaultConfiguration---------------------------------------------------------------");
    	this.insertConfiguration("default_one", "BASIC", basic);
    	this.insertConfiguration("default_two", "ADVANCED", advanced);
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
	
	public void insertConfiguration(String name, String type, Pcr[] values) throws SQLException {
		sql = "INSERT INTO CONFIG (NAME, TYPE) VALUES (?,?)";
		pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		pStatement.setString(1, name);
		pStatement.setString(2, type);
 		pStatement.executeUpdate();
 		pStatement.close();
 		LOG.debug("INSERT CONFIG----------------------------------------------------------------------------");
 		LOG.debug(name);
 		this.insertPcrValues(values, pStatement.getGeneratedKeys().getLong(1));
	}
	
	private void insertPcrValues(Pcr[] values, long key) throws SQLException {
		for(int i = 0; i < values.length; i++) {
			LOG.debug("INSERT PCR order:" + values[i].getOrder() + " value " + values[i].getValue());
			sql = "INSERT INTO PCR (SEQ, VALUE, CID) VALUES  (?,?,?)";
			pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pStatement.setInt(1, values[i].getOrder());
			pStatement.setString(2, values[i].getValue());
			pStatement.setLong(3, key);
	 		pStatement.executeUpdate();
	 		pStatement.close();
		}
	}
	
	public String getConfigurationList() throws SQLException {
		List<Configuration> ll = new LinkedList<Configuration>();
		ResultSet rs = statement.executeQuery("SELECT * FROM CONFIG");
		while (rs.next()) {
			ll.add(new Configuration(rs.getLong("ID"), rs.getString("NAME"), rs.getString("TYPE")));
		}
		return gson.toJson(ll);
	}

	public String getConfiguration(long id) {
		Configuration c;
		List<Pcr> values = new ArrayList<Pcr>();
		String sql1 = "select * from CONFIG where ID = ?;";
		String sql2 = "select * from PCR where CID = ?;";
		PreparedStatement pStatement1;
		PreparedStatement pStatement2;
		try {
			pStatement1 = connection.prepareStatement(sql1);
			pStatement2 = connection.prepareStatement(sql2);
			pStatement1.setLong(1, id);
			pStatement2.setLong(1, id);
			ResultSet rs1 = pStatement1.executeQuery();
			ResultSet rs2 = pStatement2.executeQuery();
			if(rs1.next()) {
				while(rs2.next()) {
					values.add(new Pcr(rs2.getInt("SEQ"), rs2.getString("VALUE")));
				}
				if(values.size() > 0) {
					c = new Configuration(rs1.getLong("ID"), rs1.getString("NAME"), rs1.getString("TYPE"), values.toArray(new Pcr[values.size()]));
				}
				else {
					c = new Configuration(rs1.getLong("ID"), rs1.getString("NAME"), rs1.getString("TYPE"));
				}
				pStatement1.close();
				pStatement2.close();			
				rs1.close();
				rs2.close();
				return gson.toJson(c);
			}
			else {
				return gson.toJson("");
			}

		} catch (SQLException e) {
			return gson.toJson(e.getMessage());
		}
	}
}