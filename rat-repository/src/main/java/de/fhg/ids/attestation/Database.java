/*-
 * ========================LICENSE_START=================================
 * rat-repository
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {

  private static final Logger LOG = LoggerFactory.getLogger(Database.class);
  private Connection connection;
  private String sql;
  private static final String ZERO =
      "0000000000000000000000000000000000000000000000000000000000000000";
  private static final String FFFF =
      "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

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

    for (int i = 0; i < numBasic; i++) {
      basic[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
    }
    for (int i = 0; i < numAdvanced; i++) {
      advanced[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
    }
    for (int i = 0; i < numAll; i++) {
      if (i < numAdvanced || i == numAll - 1) {
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
      LOG.debug(
          "Sorry, couldn't found JDBC driver. Make sure you have added JDBC Maven Dependency Correctly");
      e.printStackTrace();
      return;
    }
    try {
      connection = DriverManager.getConnection("jdbc:sqlite:configuration.db");
      LOG.debug("connection to sqlite db successful!");
    } catch (SQLException e) {
      LOG.debug("Failed to make connection to mysql db!", e);
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public void createTables() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      sql =
          "DROP TABLE IF EXISTS CONFIG; CREATE TABLE CONFIG ("
              + "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "'NAME' VARCHAR(255) NULL DEFAULT 'Configuration Name',"
              + "'TYPE' VARCHAR(255) NULL DEFAULT 'BASIC');";
      statement.executeUpdate(sql);
    }

    try (Statement statement = connection.createStatement()) {
      sql =
          "DROP TABLE IF EXISTS PCR; CREATE TABLE PCR ("
              + "'ID' INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "'SEQ' INTEGER DEFAULT '0',"
              + "'VALUE' CHAR(64) NOT NULL DEFAULT '"
              + ZERO
              + "',"
              + "'CID' INTEGER NOT NULL,"
              + "FOREIGN KEY (CID) REFERENCES 'CONFIG' ('ID'));";
      statement.executeUpdate(sql);
    }
  }

  public long insertConfiguration(String name, String type, Pcr[] values) throws SQLException {
    sql = "INSERT INTO CONFIG (NAME, TYPE) VALUES (?,?)";
    try (PreparedStatement pStatement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      pStatement.setString(1, name);
      pStatement.setString(2, type);
      pStatement.executeUpdate();
      long key = pStatement.getGeneratedKeys().getLong(1);
      this.insertPcrs(values, key);
      return key;
    }
  }

  private void insertPcrs(Pcr[] values, long key) throws SQLException {
    sql = "INSERT INTO PCR (SEQ, VALUE, CID) VALUES  (?,?,?)";
    try (PreparedStatement pStatement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      for (Pcr value : values) {
        LOG.debug("INSERT PCR order:" + value.getNumber() + " value " + value.getValue());
        pStatement.setInt(1, value.getNumber());
        pStatement.setString(2, value.getValue());
        pStatement.setLong(3, key);
        pStatement.executeUpdate();
      }
    }
  }

  private Long[] getConfigurationIdSingle(Pcr value) throws SQLException {
    sql =
        "SELECT * FROM CONFIG INNER JOIN PCR ON PCR.CID = CONFIG.ID WHERE PCR.SEQ = ? AND PCR.VALUE = ? ORDER BY CONFIG.ID";
    try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
      pStatement.setInt(1, value.getNumber());
      pStatement.setString(2, value.getValue());
      List<Long> result = new ArrayList<Long>();
      try (ResultSet rs = pStatement.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getLong("ID"));
        }
        pStatement.close();
      }
      return result.toArray(new Long[result.size()]);
    }
  }

  public Long[] getConfigurationId(Pcr[] values) throws SQLException {
    Long[] start = this.getConfigurationIdSingle(values[0]);
    values = Arrays.copyOfRange(values, 1, values.length);
    LOG.debug(
        "\n#################################\nstart:"
            + Arrays.toString(start)
            + "\n#################################\n");
    for (Pcr value : values) {
      Long[] now = this.getConfigurationIdSingle(value);
      LOG.debug(
          "\n#################################\nnow:"
              + Arrays.toString(now)
              + "\n#################################\n");
      start = intersection(start, now);
    }
    LOG.debug(
        "\n#################################\nfinal:"
            + Arrays.toString(start)
            + "\n#################################\n");
    return start;
  }

  private Long[] intersection(Long[] a, Long[] b) {
    ArrayList<Long> listOne = new ArrayList<>(Arrays.asList(a));
    ArrayList<Long> listTwo = new ArrayList<>(Arrays.asList(b));
    listOne.retainAll(listTwo);
    return listOne.toArray(new Long[listOne.size()]);
  }

  public boolean deleteConfigurationById(long id) throws SQLException {
    sql = "DELETE FROM CONFIG WHERE ID = ?";
    int rowCount;
    try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
      pStatement.setLong(1, id);
      rowCount = pStatement.executeUpdate();
    }
    if (rowCount == 1) {
      sql = "DELETE FROM PCR WHERE CID = ?";
      try (PreparedStatement pStatement = connection.prepareStatement(sql)) {
        pStatement.setLong(1, id);
        pStatement.executeUpdate();
      }
    }
    return rowCount == 1;
  }

  public Configuration[] getConfigurationList() throws SQLException {
    List<Configuration> ll = new LinkedList<Configuration>();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM CONFIG")) {
      while (rs.next()) {
        ll.add(this.getConfiguration(rs.getLong("ID")));
      }
    }
    return ll.toArray(new Configuration[ll.size()]);
  }

  public Configuration getConfiguration(long id) {
    Configuration c;
    List<Pcr> values = new ArrayList<Pcr>();
    String sql1 = "select * from CONFIG where ID = ?;";
    String sql2 = "select * from PCR where CID = ?;";
    try (PreparedStatement pStatement1 = connection.prepareStatement(sql1);
        PreparedStatement pStatement2 = connection.prepareStatement(sql2)) {
      pStatement1.setLong(1, id);
      pStatement2.setLong(1, id);
      try (ResultSet rs1 = pStatement1.executeQuery();
          ResultSet rs2 = pStatement2.executeQuery()) {
        if (rs1.next()) {
          while (rs2.next()) {
            values.add(
                Pcr.newBuilder()
                    .setNumber(rs2.getInt("SEQ"))
                    .setValue(rs2.getString("VALUE"))
                    .build());
          }
          if (values.size() > 0) {
            c =
                new Configuration(
                    rs1.getLong("ID"),
                    rs1.getString("NAME"),
                    rs1.getString("TYPE"),
                    values.toArray(new Pcr[values.size()]));
          } else {
            c = new Configuration(rs1.getLong("ID"), rs1.getString("NAME"), rs1.getString("TYPE"));
          }
          return c;
        } else {
          return null;
        }
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
      if (configIds.length == 1) {
        return true;
      } else if (configIds.length > 1) {
        LOG.debug(
            "found more than one matching configuration ("
                + Arrays.toString(configIds)
                + ") =( this shouldn't happen !");
        return true;
      } else {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }
  }
}
