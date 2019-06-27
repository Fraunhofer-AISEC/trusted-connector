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
package de.fhg.aisec.ids.attestation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import org.apache.camel.test.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/** Unit test for ratRepositoryTest */
public class RatRepositoryTest {

  private static RemoteAttestationServer ratServer;
  private static Pcr[] values;
  private static final int PORT = AvailablePortFinder.getNextAvailable();
  private static final String PATH = "rat-verify";
  private static HostnameVerifier hv;
  private static final Logger LOG = LoggerFactory.getLogger(RatRepositoryTest.class);
  private static final String sURL = "https://127.0.0.1:" + PORT + "/" + PATH;
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

  @BeforeClass
  public static void initRepo() {
    ratServer = new RemoteAttestationServer("127.0.0.1", PATH, PORT);
    ratServer.start();

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}
          }
        };
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, null);
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
      System.out.println("Error" + e);
    }
    hv =
        (urlHostName, session) -> {
          System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
          return true;
        };
  }

  @AfterClass
  public static void closeRepo() {
    ratServer = null;
  }

  @Test
  public void testURL() throws MalformedURLException {
    assertEquals(sURL, ratServer.getURI().toURL().toString());
  }

  @Test
  public void testDatabaseIsRunning() throws SQLException {
    assertFalse(ratServer.getDatabase().getConnection().isClosed());
  }

  @Test
  public void testDefaultConfiguration() throws SQLException, IOException {
    Gson gson =
        new GsonBuilder()
            .registerTypeAdapter(
                ByteString.class,
                (JsonDeserializer<ByteString>)
                    (json, typeOfT, context) -> {
                      JsonObject jsonObject = json.getAsJsonObject();
                      byte[] bytes = context.deserialize(jsonObject.get("bytes"), byte[].class);
                      return ByteString.copyFrom(bytes);
                    })
            .create();
    List<Configuration> config =
        gson.fromJson(
            Files.newBufferedReader(
                FileSystems.getDefault().getPath("src/test/resources/configurationList.json"),
                StandardCharsets.UTF_8),
            new TypeToken<List<Configuration>>() {}.getType());
    assertEquals(config, ratServer.getDatabase().getConfigurationList());
  }

  @Test
  public void testBASICConfiguration() {
    // this tests the BASIC pcr values configuration 0-10
    long id = new java.util.Random().nextLong();
    values = new Pcr[12];
    for (int i = 0; i < 12; i++) {
      values[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
    }
    ConnectorMessage msg =
        ConnectorMessage.newBuilder()
            .setId(id)
            .setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
            .setAttestationRepositoryRequest(
                AttestationRepositoryRequest.newBuilder()
                    .setAtype(IdsAttestationType.BASIC)
                    .addAllPcrValues(Arrays.asList(values))
                    .build())
            .build();
    ConnectorMessage result = this.send(msg);

    assertNotNull(result);
    assertEquals(id + 1, result.getId());
    assertEquals(ConnectorMessage.Type.RAT_REPO_RESPONSE, result.getType());
    assertEquals(IdsAttestationType.BASIC, result.getAttestationRepositoryResponse().getAtype());
    assertTrue(result.getAttestationRepositoryResponse().getResult());
  }

  @Test
  public void testADVANCEDConfiguration() {
    // this tests the ADVANCED pcr values configuration in this case with 5 values: PCR0, PCR2,
    // PCR4, PCR6, PCR8
    long id = new java.util.Random().nextLong();
    values = new Pcr[17];
    for (int i = 0; i < 17; i++) {
      values[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
    }
    ConnectorMessage msg =
        ConnectorMessage.newBuilder()
            .setId(id)
            .setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
            .setAttestationRepositoryRequest(
                AttestationRepositoryRequest.newBuilder()
                    .setAtype(IdsAttestationType.ADVANCED)
                    .addAllPcrValues(Arrays.asList(values))
                    .build())
            .build();
    ConnectorMessage result = this.send(msg);

    assertNotNull(result);
    assertEquals(id + 1, result.getId());
    assertEquals(ConnectorMessage.Type.RAT_REPO_RESPONSE, result.getType());
    assertEquals(IdsAttestationType.ADVANCED, result.getAttestationRepositoryResponse().getAtype());
    assertTrue(result.getAttestationRepositoryResponse().getResult());
  }

  @Test
  public void testInvalidALLConfiguration() {
    // this tests ALL pcr values
    long id = new java.util.Random().nextLong();
    values = new Pcr[24];
    for (int i = 0; i < 24; i++) {
      values[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
    }
    ConnectorMessage msg =
        ConnectorMessage.newBuilder()
            .setId(id)
            .setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
            .setAttestationRepositoryRequest(
                AttestationRepositoryRequest.newBuilder()
                    .setAtype(IdsAttestationType.ALL)
                    .addAllPcrValues(Arrays.asList(values))
                    .build())
            .build();
    ConnectorMessage result = this.send(msg);

    assertNotNull(result);
    assertEquals(id + 1, result.getId());
    assertEquals(ConnectorMessage.Type.RAT_REPO_RESPONSE, result.getType());
    assertEquals(IdsAttestationType.ALL, result.getAttestationRepositoryResponse().getAtype());
    assertFalse(result.getAttestationRepositoryResponse().getResult());
  }

  @Test
  public void testValidALLConfiguration() {
    // this tests ALL pcr values
    int numAll = 24;
    long id = new java.util.Random().nextLong();
    values = new Pcr[numAll];
    for (int i = 0; i < numAll; i++) {
      if (i < 17 || i == numAll - 1) {
        values[i] = Pcr.newBuilder().setNumber(i).setValue(ZERO).build();
      } else {
        values[i] = Pcr.newBuilder().setNumber(i).setValue(FFFF).build();
      }
    }
    ConnectorMessage msg =
        ConnectorMessage.newBuilder()
            .setId(id)
            .setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
            .setAttestationRepositoryRequest(
                AttestationRepositoryRequest.newBuilder()
                    .setAtype(IdsAttestationType.ALL)
                    .addAllPcrValues(Arrays.asList(values))
                    .build())
            .build();
    ConnectorMessage result = this.send(msg);

    assertNotNull(result);
    assertEquals(id + 1, result.getId());
    assertEquals(ConnectorMessage.Type.RAT_REPO_RESPONSE, result.getType());
    assertEquals(IdsAttestationType.ALL, result.getAttestationRepositoryResponse().getAtype());
    assertTrue(result.getAttestationRepositoryResponse().getResult());
  }

  private ConnectorMessage send(ConnectorMessage msg) {
    try {
      HttpsURLConnection urlc = (HttpsURLConnection) new URL(sURL).openConnection();
      urlc.setHostnameVerifier(hv);
      urlc.setDoInput(true);
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Accept", "application/x-protobuf");
      urlc.setRequestProperty("Content-Type", "application/x-protobuf");
      msg.writeTo(urlc.getOutputStream());
      LOG.debug(
          "\n######################################\n"
              + msg.toString()
              + "######################################\n");
      ConnectorMessage result =
          ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
      LOG.debug(
          "\n######################################\n"
              + result.toString()
              + "######################################\n");
      return result;
    } catch (Exception e) {
      LOG.debug("Exception : " + e.toString());
    }
    return null;
  }
}
