/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import de.fhg.aisec.ids.webconsole.api.data.Cert;
import de.fhg.aisec.ids.webconsole.api.data.Identity;
import de.fhg.aisec.ids.webconsole.api.data.User;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.*;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assume.assumeFalse;

public class RestApiTests extends Assert {
  private static final String ENDPOINT_ADDRESS = "local://testserver";
  private static Server server;

  @BeforeClass
  public static void initialize() {
    startServer();
  }

  /** Starts a test server. Note that REST endpoints must be registered manually here. */
  private static void startServer() {
    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setResourceClasses(CertApi.class);
    sf.setResourceClasses(MetricAPI.class);
    sf.setResourceClasses(UserApi.class);

    // Server uses Jackson for JSON mapping
    List<Object> providers = new ArrayList<>();
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(new ObjectMapper());
    providers.add(jackson);
    providers.add(new JWTRestAPIFilter());
    // add custom providers if any
    sf.setProviders(providers);

    sf.setResourceProvider(CertApi.class, new SingletonResourceProvider(new CertApi(), true));
    sf.setResourceProvider(MetricAPI.class, new SingletonResourceProvider(new MetricAPI(), true));
    sf.setResourceProvider(UserApi.class, new SingletonResourceProvider(new UserApi(), true));
    sf.setAddress(ENDPOINT_ADDRESS);

    server = sf.create();
  }

  private WebClient newClient(String... token) {
    // Client uses Jackson for JSON mapping
    JacksonJsonProvider jackson = new JacksonJsonProvider();
    jackson.setMapper(new ObjectMapper());
    WebClient client = WebClient.create(ENDPOINT_ADDRESS, Collections.singletonList(jackson));
    WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
    if (token != null && token.length > 0) {
      client.header("Authorization", "Bearer: " + token[0]);
    }
    return client;
  }

  @AfterClass
  public static void destroy() {
    server.stop();
    server.destroy();
  }

  @Before
  public void before() {
    CertApi certApi = new CertApi();

    Identity idSpec = new Identity();
    idSpec.c = "c";
    idSpec.cn = "common name";
    idSpec.l = "location";
    idSpec.s = "subject";
    certApi.createIdentity(idSpec);
  }

  @Test
  public void testListCerts() {
    String token = login();

    WebClient client = newClient(token);
    client.accept(MediaType.APPLICATION_JSON);
    client.path("/certs/list_certs");
    List<Cert> certs = client.get(new GenericType<>() {
    });
    assertNotNull(certs);
    assertTrue(certs.size() > 0);
  }

  @Test
  public void testListIdentities() {
    String token = login();

    WebClient client = newClient(token);
    client.accept(MediaType.APPLICATION_JSON);
    client.path("/certs/list_identities");
    List<Cert> keys = client.get(new GenericType<>() {
    });
    assertNotNull(keys);
    assertTrue(keys.size() > 0);
  }

  @Test
  public void testCreateIdentity() {
    String token = login();

    WebClient client = newClient(token);

    Identity idSpec = new Identity();
    idSpec.c = "c";
    idSpec.cn = "common name";
    idSpec.l = "location";
    idSpec.s = "subject";

    client.accept(MediaType.APPLICATION_JSON);
    client.header("Content-type", MediaType.APPLICATION_JSON);
    client.path("/certs/create_identity");
    String alias = client.post(idSpec, String.class);
    assertTrue(alias.length() > 5);
  }

  @Test
  public void testDeleteIdentity() {
    String token = login();

    // Get list of identities
    WebClient client = newClient(token);
    client.accept(MediaType.APPLICATION_JSON);
    client.path("/certs/list_identities");
    List<Cert> certs = client.get(new GenericType<>() {
    });
    assumeFalse(certs.isEmpty());

    // Choose an identity and delete it
    client = newClient(token);
    String alias = certs.get(0).alias;
    client.header("Content-type", MediaType.APPLICATION_JSON);
    client.path("/certs/delete_identity");
    Response resp = client.post(alias);
    assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    // Confirm it has been deleted
    client = newClient(token);
    client.path("/certs/list_identities");
    List<Cert> keys = client.get(new GenericType<>() {
    });
    boolean contained = false;
    for (Cert k : keys) {
      contained |= alias.equals(k.alias);
    }
    assertFalse(contained);
  }

  @Test
  public void deleteCerts() {
    String token = login();

    // Get list of certs
    WebClient client = newClient(token);
    client.accept(MediaType.APPLICATION_JSON);
    client.path("/certs/list_certs");
    List<Cert> certs = client.get(new GenericType<>() {
    });
    assumeFalse(certs.isEmpty());

    // Choose a cert and delete it
    client = newClient(token);
    String alias = certs.get(0).alias;
    client.header("Content-type", MediaType.APPLICATION_JSON);
    client.path("/certs/delete_cert");
    Response resp = client.post(alias);
    assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    // Confirm it has been deleted
    client = newClient(token);
    client.path("/certs/list_certs");
    List<Cert> keys = client.get(new GenericType<>() {
    });
    boolean contained = false;
    for (Cert k : keys) {
      contained |= alias.equals(k.alias);
    }
    assertFalse(contained);
  }

  @Test
  public void getMetrics() {
    String token = login();

    // Access a protected endpoint
    WebClient c = newClient(token);
    c.accept(MediaType.APPLICATION_JSON);
    c.path("/metric/get");
    Map<String, String> metrics = c.get(new GenericType<>() {
    });
    assumeFalse(metrics.isEmpty());
  }

  /**
   * Retrieves a fresh JWT from server.
   *
   * @return The generated authentication token
   */
  private String login() {
    WebClient c = newClient();
    c.path("/user/login");
    c.accept(MediaType.APPLICATION_JSON);
    c.header("Content-type", MediaType.APPLICATION_JSON);
    User u = new User();
    u.username = "ids";
    u.password = "ids";
    Map<String, String> result = c.post(u, new GenericType<>() {});
    String token = result.get("token");
    c.header("Authorization", "Bearer: " + token);
    return token;
  }
}
