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

import com.google.protobuf.ByteString;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Database {

	private static final Logger LOG = LoggerFactory.getLogger(Database.class);
	private Connection connection;
	private String sql;
	private static final int SHA256_BYTES_LEN = 32;
	private static final ByteString ZERO;
	private static final ByteString FFFF;

	static {
		// Initialize example PCR constants
		byte[] bytes = new byte[SHA256_BYTES_LEN];
		Arrays.fill(bytes, (byte) 0x00);
		ZERO = ByteString.copyFrom(bytes);
		Arrays.fill(bytes, (byte) 0xff);
		FFFF = ByteString.copyFrom(bytes);
	}
 
	public Database() {
		try {
			makeJDBCConnection();
			createTables();
			insertDefaultConfiguration();
		} catch(SQLException e) {
			throw new IllegalStateException(e);
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
			basic[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
		}
		for (int i = 0; i < numAdvanced ; i++) {
			advanced[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
		}
		for (int i = 0; i < numAll ; i++) {
			if(i < numAdvanced || i == numAll - 1) {
				all[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
			} else {
				all[i] = Pcr.newBuilder().setNumber(i).setValue(FFFF).build();
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
			LOG.error("Sorry, couldn't found JDBC driver. Make sure you have added JDBC Maven Dependency Correctly", e);
			return;
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:configuration.db");
			LOG.debug("connection to sqlite db successful!");
		} catch (SQLException e) {
			LOG.error("Failed to make connection to mysql db!", e);
		}

	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void createTables() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			sql = "DROP TABLE IF EXISTS CONFIG; CREATE TABLE CONFIG ("
					+ "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "'NAME' VARCHAR(255) NULL DEFAULT 'Configuration Name',"
					+ "'TYPE' VARCHAR(255) NULL DEFAULT 'BASIC');";
			statement.executeUpdate(sql);
		}
		
		try (Statement statement = connection.createStatement()) {
			sql = "DROP TABLE IF EXISTS PCR; CREATE TABLE PCR ("
					+ "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "'SEQ' INTEGER DEFAULT '0',"
					+ "'VALUE' BLOB NOT NULL,"
					+ "'CID' INTEGER NOT NULL,"
					+ "FOREIGN KEY (CID) REFERENCES 'CONFIG' ('ID'));";
			statement.executeUpdate(sql);
		}
	}
	
	public long insertConfiguration(String name, String type, Pcr[] values) throws SQLException {
		sql = "INSERT INTO CONFIG (NAME, TYPE) VALUES (?,?)";
		try (PreparedStatement pStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			pStatement.setString(1, name);
			pStatement.setString(2, type);
			pStatement.executeUpdate();
			try (ResultSet keySet = pStatement.getGeneratedKeys()) {
				long key = keySet.getLong(1);
				this.insertPcrs(values, key);
				return key;
			}
		}
	}
	
	private void insertPcrs(Pcr[] values, long key) throws SQLException {
		sql = "INSERT INTO PCR (SEQ, VALUE, CID) VALUES  (?,?,?)";
		try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
			for (Pcr value : values) {
				LOG.debug("INSERT PCR order: {} value {}", value.getNumber(), value.getValue());
				pStatement.setInt(1, value.getNumber());
				pStatement.setBytes(2, value.getValue().toByteArray());
				pStatement.setLong(3, key);
				pStatement.executeUpdate();
			}
		}
	}
	
	private List<Long> getConfigurationIdSingle(Pcr value) throws SQLException {
		sql = "SELECT * FROM CONFIG INNER JOIN PCR ON PCR.CID = CONFIG.ID " +
				"WHERE PCR.SEQ = ? AND PCR.VALUE = ? ORDER BY CONFIG.ID";
		try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
			pStatement.setInt(1, value.getNumber());
			pStatement.setBytes(2, value.getValue().toByteArray());
			List<Long> result = new ArrayList<>();
			try (ResultSet rs = pStatement.executeQuery()) {
				while (rs.next()) {
					result.add(rs.getLong("ID"));
				}
			}
			return result;
		}
	}
	
	public List<Long> getConfigurationId(List<Pcr> values) throws SQLException {
		List<Long> start = this.getConfigurationIdSingle(values.get(0));
		values = values.subList(1, values.size());
		if (LOG.isDebugEnabled()) {
			LOG.debug("\n#################################\nstart:{}\n#################################\n",
					Arrays.toString(start.toArray()));
		}
		for(Pcr value: values){
			List<Long> now =  this.getConfigurationIdSingle(value);
			if (LOG.isDebugEnabled()) {
				LOG.debug("\n#################################\nnow:{}\n#################################\n",
						Arrays.toString(now.toArray()));
			}
			start = intersection(start, now);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("\n#################################\nfinal:{}\n#################################\n",
					Arrays.toString(start.toArray()));
		}
		return start;
	}

	private List<Long> intersection(List<Long> a, List<Long> b){
		ArrayList<Long> result = new ArrayList<>(a);
		result.retainAll(b);
		return result;
	}

	public boolean deleteConfigurationById(long id) throws SQLException {
		sql = "DELETE FROM CONFIG WHERE ID = ?";
		int rowCount;
		try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
			pStatement.setLong(1, id);
			rowCount = pStatement.executeUpdate();
		}
		if(rowCount == 1) {
			sql = "DELETE FROM PCR WHERE CID = ?";
			try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
				pStatement.setLong(1, id);
				pStatement.executeUpdate();
			}
		}
		return rowCount == 1;
	}
	
	public List<Configuration> getConfigurationList() throws SQLException {
		List<Configuration> ll = new LinkedList<>();
		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT * FROM CONFIG")) {
			while (rs.next()) {
				long id = rs.getLong("ID");
				ll.add(this.getConfiguration(id));
			}
		}
		return ll;
	}

	public Configuration getConfiguration(long id) {
		Configuration c;
		List<Pcr> values = new ArrayList<>();
		String sql1 = "select * from CONFIG where ID = ?;";
		String sql2 = "select * from PCR where CID = ?;";
		try (PreparedStatement pStatement1 = connection.prepareStatement(sql1);
			 PreparedStatement pStatement2 = connection.prepareStatement(sql2)) {
			pStatement1.setLong(1, id);
			pStatement2.setLong(1, id);
			try (ResultSet rs1 = pStatement1.executeQuery();
				 ResultSet rs2 = pStatement2.executeQuery()) {
				if(rs1.next()) {
					while(rs2.next()) {
						values.add(Pcr.newBuilder().setNumber(rs2.getInt("SEQ"))
								.setValue(ByteString.copyFrom(rs2.getBytes("VALUE"))).build());
					}
					if(!values.isEmpty()) {
						c = new Configuration(rs1.getLong("ID"), rs1.getString("NAME"),
								rs1.getString("TYPE"), values.toArray(new Pcr[0]));
					} else {
						c = new Configuration(rs1.getLong("ID"), rs1.getString("NAME"),
								rs1.getString("TYPE"));
					}
					return c;
				} else {
					return null;
				}
			}
		} catch (SQLException e) {
			LOG.error("Error in getConfiguration()", e);
			return null;
		}
	}

	
	public boolean checkMessage(ConnectorMessage message) {
		try {
			List<Pcr> pcrList = message.getAttestationRepositoryRequest().getPcrValuesList();
			List<Long> configIds = this.getConfigurationId(pcrList);
			if(configIds.size() == 1) {
				return true;
			} else if(configIds.size() > 1) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("found more than one matching configuration ({}) =( this shouldn't happen!",
							Arrays.toString(configIds.toArray()));
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
	}
}
