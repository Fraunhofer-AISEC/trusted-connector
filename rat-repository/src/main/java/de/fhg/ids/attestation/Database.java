/*-
 * ========================LICENSE_START=================================
 * rat-repository
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.attestation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

public class Database {
	
	private static PreparedStatement pStatement = null;
	private Logger LOG = LoggerFactory.getLogger(Database.class);
	private Connection connection;
	private Statement statement;
	private String sql;
	private String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	private String fff =  "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
 
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
		int numBasic = 11;
		int numAdvanced = 17;
		int numAll = 24;
		
		Pcr[] basic = new Pcr[numBasic];
		Pcr[] advanced = new Pcr[numAdvanced];
		Pcr[] all = new Pcr[numAll];
		
		for (int i = 0; i < numBasic ; i++) {
			basic[i] = Pcr.newBuilder().setNumber(i).setValue(zero).build();
		}
		for (int i = 0; i < numAdvanced ; i++) {
			advanced[i] = Pcr.newBuilder().setNumber(i).setValue(zero).build();
		}
		for (int i = 0; i < numAll ; i++) {
			if(i < numAdvanced || i == numAll - 1) {
				all[i] = Pcr.newBuilder().setNumber(i).setValue(zero).build();
			}
			else {
				all[i] = Pcr.newBuilder().setNumber(i).setValue(fff).build();
			}
		}		
    	this.insertConfiguration("default_basic", "BASIC", basic);
    	this.insertConfiguration("default_advanced", "ADVANCED", advanced);
    	this.insertConfiguration("default_all", "ALL", all);
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
	
	public long insertConfiguration(String name, String type, Pcr[] values) throws SQLException {
		sql = "INSERT INTO CONFIG (NAME, TYPE) VALUES (?,?)";
		pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		pStatement.setString(1, name);
		pStatement.setString(2, type);
 		pStatement.executeUpdate();
 		pStatement.close();
 		long key = pStatement.getGeneratedKeys().getLong(1);
 		this.insertPcrs(values, key);
 		return key;
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
	
	private Long[] getConfigurationIdSingle(Pcr value) throws SQLException {
		sql = "SELECT * FROM CONFIG INNER JOIN PCR ON PCR.CID = CONFIG.ID WHERE PCR.SEQ = ? AND PCR.VALUE = ? ORDER BY CONFIG.ID";
		pStatement = connection.prepareStatement(sql);
		pStatement.setInt(1, value.getNumber());
		pStatement.setString(2, value.getValue());
		ResultSet rs = pStatement.executeQuery();
		List<Long> result = new ArrayList<Long>();
		while(rs.next()) {
			result.add(rs.getLong("ID"));
		}
 		pStatement.close();
 		rs.close();
 		return result.toArray(new Long[result.size()]);
	}
	
	public Long[] getConfigurationId(Pcr[] values) throws SQLException {
		Long[] start = this.getConfigurationIdSingle(values[0]);
		values = Arrays.copyOfRange(values, 1, values.length);
		LOG.debug("\n#################################\nstart:" + Arrays.toString(start) + "\n#################################\n");
		for(Pcr value: values){
			Long[] now =  this.getConfigurationIdSingle(value);
			LOG.debug("\n#################################\nnow:" + Arrays.toString(now) +"\n#################################\n");
			start = intersection(start, now);
		}
		LOG.debug("\n#################################\nfinal:" + Arrays.toString(start) +"\n#################################\n");
		return start;
	}
	
	private Long[] intersection(Long[] a, Long[] b){
		Collection listOne = new ArrayList(Arrays.asList(a));
		Collection listTwo = new ArrayList(Arrays.asList(b));
		listOne.retainAll(listTwo);
		return (Long[]) listOne.toArray(new Long[listOne.size()]);
	}

	public boolean deleteConfigurationById(long id) throws SQLException {
		sql = "DELETE FROM CONFIG WHERE ID = ?";
		pStatement = connection.prepareStatement(sql);
		pStatement.setLong(1, id);
 		int val = pStatement.executeUpdate();
 		pStatement.close();
 		if(val==1) {
 	 		sql = "DELETE FROM PCR WHERE CID = ?";
 	 		pStatement = connection.prepareStatement(sql);
 			pStatement.setLong(1, id);
 	 		pStatement.executeUpdate();
 	 		pStatement.close();
 			return true;
 		}
 		else {
 			return false;
 		}
	}
	
	public Configuration[] getConfigurationList() throws SQLException {
		List<Configuration> ll = new LinkedList<Configuration>();
		ResultSet rs = statement.executeQuery("SELECT * FROM CONFIG");
		while (rs.next()) {
			ll.add(this.getConfiguration(rs.getLong("ID")));
		}
		return ll.toArray(new Configuration[ll.size()]);
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

	
	public boolean checkMessage(ConnectorMessage message) {
		try {
			List<Pcr> pcrList = message.getAttestationRepositoryRequest().getPcrValuesList();
			Pcr[] values = pcrList.toArray(new Pcr[pcrList.size()]);
			Long[] configIds = this.getConfigurationId(values);
			if(configIds.length == 1) {
				return true;
			}
			else if(configIds.length > 1) {
				LOG.debug("found more than one matching configuration ("+configIds.toString()+") =( this shouldn't happen !");
				return true;
			}
			else {
				return false;
			}
		}
		catch(Exception ex) {
			return false;
		}
	}
}
