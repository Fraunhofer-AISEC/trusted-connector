package de.fhg.ids.attestation;

import static java.lang.Math.toIntExact;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sqlite.jdbc4.*;

import com.google.gson.Gson;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

public class Database {
	
	private static PreparedStatement pStatement = null;
	private Gson gson = new Gson();
	private Logger LOG = LoggerFactory.getLogger(Database.class);
	private Connection connection;
	private Statement statement;
	private String sql;
	private String zero = "0000000000000000000000000000000000000000000000000000000000000000";
 
	public Database() {
		makeJDBCConnection();
		try {
			createTables();
		} catch (SQLException e) {
			LOG.debug("ERROR: could not create Tables !");
			e.printStackTrace();
		}
		try {
			insertDefaultConfiguration();
		} catch (SQLException e) {
			LOG.debug("ERROR: could not insert default Configuration !");
			e.printStackTrace();
		}
	}

	private void insertDefaultConfiguration() throws SQLException {
		Pcr[] basic = new Pcr[10];
		Pcr[] advanced = new Pcr[24];
		for (int i = 0; i < 10 ; i++) {
			basic[i] = Pcr.newBuilder().setNumber(i).setValue(zero).build();
		}
		for (int i = 0; i < 24 ; i++) {
			advanced[i] = Pcr.newBuilder().setNumber(i).setValue(zero).build();
		}
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
				LOG.debug("connection to sqlite db successful!");
			} else {
				LOG.debug("Failed to make connection to mysql db!");
			}
		} catch (SQLException e) {
			LOG.debug(e.getMessage());
			return;
		}

	}
	
	public Connection getConnection() {
		return connection;
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
 		this.insertPcrs(values, pStatement.getGeneratedKeys().getLong(1));
	}
	
	private void insertPcrs(Pcr[] values, long key) throws SQLException {
		for(int i = 0; i < values.length; i++) {
			LOG.debug("INSERT PCR order:" + values[i].getNumber() + " value " + values[i].getValue());
			sql = "INSERT INTO PCR (SEQ, VALUE, CID) VALUES  (?,?,?)";
			pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pStatement.setInt(1, values[i].getNumber());
			pStatement.setString(2, values[i].getValue());
			pStatement.setLong(3, key);
	 		pStatement.executeUpdate();
	 		pStatement.close();
		}
	}
	
	private Configuration[] getConfigurationIdBy(Pcr value) throws SQLException {
		Configuration c = null;
		sql = "SELECT * FROM CONFIG INNER JOIN PCR ON PCR.CID = CONFIG.ID WHERE PCR.SEQ = ? AND PCR.VALUE = ? ORDER BY CONFIG.ID";
		pStatement = connection.prepareStatement(sql);
		pStatement.setInt(1, value.getNumber());
		pStatement.setString(2, value.getValue());
		ResultSet rs = pStatement.executeQuery();
		List<Configuration> ids = new LinkedList<Configuration>();
		while(rs.next()) {
			ids.add(new Configuration(rs.getLong("ID"), rs.getString("NAME"), rs.getString("TYPE")));
		}
 		pStatement.close();
 		rs.close();
 		return ids.toArray(new Configuration[ids.size()]);
	}


	public boolean deleteConfigurationById(long id) throws SQLException {
		LOG.debug("DELETE FROM CONFIG WHERE ID = " + id);
		sql = "DELETE FROM CONFIG WHERE ID = ?";
		pStatement = connection.prepareStatement(sql);
		pStatement.setLong(1, id);
 		int val = pStatement.executeUpdate();
 		pStatement.close();
 		if(val==1) {
 			return true;
 		}
 		else {
 			return false;
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

	public Configuration getConfiguration(long id) {
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
					values.add(Pcr.newBuilder().setNumber(rs2.getInt("SEQ")).setValue(rs2.getString("VALUE")).build());
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
				return c;
			}
			else {
				return null;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public boolean checkMessage(ConnectorMessage message) throws SQLException {
		Configuration tmp;
		Set<Long> names = new HashSet<Long>();
		Configuration[] validConfigs = this.getConfigurationIdBy(message.getAttestationRepositoryRequest().getPcrValues(0));
		this.computeHashSet(validConfigs, names, true);
		for(int i = 2; i < message.getAttestationRepositoryRequest().getPcrValuesCount(); i++) {
			this.computeHashSet(this.getConfigurationIdBy(message.getAttestationRepositoryRequest().getPcrValues(i)), names, false);
		}
		if(names.size() > 0) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private void computeHashSet(Configuration[] configs, Set<Long> ids, boolean initial) {
		if(initial) {
			for (Configuration c: configs) {
				ids.add(c.getId());
			}
		}
		else {
			for (Configuration c: configs) {
				if (ids.contains(c.getId())) {
					continue;
				} else {
					ids.remove(c.getId());
				}
			}
		}
	}
}