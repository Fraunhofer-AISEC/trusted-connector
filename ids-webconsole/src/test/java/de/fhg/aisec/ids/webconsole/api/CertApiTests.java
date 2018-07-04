/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole.api;

import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import de.fhg.aisec.ids.webconsole.api.data.Cert;
import de.fhg.aisec.ids.webconsole.api.data.Identity;

public class CertApiTests extends Assert {
	private final static String ENDPOINT_ADDRESS = "local://certs";
	private static Server server;

	@BeforeClass
	public static void initialize() throws Exception {
		startServer();
	}

	private static void startServer() throws Exception {
		JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
		sf.setResourceClasses(CertApi.class);

		// Server uses Jackson for JSON mapping
		List<Object> providers = new ArrayList<Object>();
		JacksonJsonProvider jackson = new JacksonJsonProvider();
		jackson.setMapper(new ObjectMapper());
		providers.add(jackson);
		// add custom providers if any
		sf.setProviders(providers);

		sf.setResourceProvider(CertApi.class, new SingletonResourceProvider(new CertApi(), true));
		sf.setAddress(ENDPOINT_ADDRESS);

		server = sf.create();
	}

	private WebClient newClient() {
		// Client uses Jackson for JSON mapping
		JacksonJsonProvider jackson = new JacksonJsonProvider();
		jackson.setMapper(new ObjectMapper());
		WebClient client = WebClient.create(ENDPOINT_ADDRESS, Collections.singletonList(jackson));
		WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
		return client;
	}

	@AfterClass
	public static void destroy() throws Exception {
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
		WebClient client = newClient();
		client.accept(MediaType.APPLICATION_JSON);
		client.path("/certs/list_certs");
		List<Cert> certs = client.get(new GenericType<List<Cert>>() {
		});
		assertNotNull(certs);
		assertTrue(certs.size() > 0);
	}

	@Test
	public void testListIdentities() {
		WebClient client = newClient();
		client.accept(MediaType.APPLICATION_JSON);
		client.path("/certs/list_identities");
		List<Cert> keys = client.get(new GenericType<List<Cert>>() {
		});
		assertNotNull(keys);
		assertTrue(keys.size() > 0);
	}

	@Test
	public void testCreateIdentity() {
		WebClient client = newClient();

		Identity idSpec = new Identity();
		idSpec.c = "c";
		idSpec.cn = "common name";
		idSpec.l = "location";
		idSpec.s = "subject";

		client.accept(MediaType.APPLICATION_JSON);
		client.header("Content-type", MediaType.APPLICATION_JSON);
		client.path("/certs/create_identity");
		String alias = client.post(idSpec,String.class);
		assertTrue(alias.length() > 5);
	}

	@Test
	public void testDeleteIdentity() {
		// Get list of identities
		WebClient client = newClient();
		client.accept(MediaType.APPLICATION_JSON);
		client.path("/certs/list_identities");
		List<Cert> certs = client.get(new GenericType<List<Cert>>() { });
		assumeFalse(certs.isEmpty());

		// Choose an identity and delete it
		client = newClient();
		String alias = certs.get(0).alias;
		client.header("Content-type", MediaType.APPLICATION_JSON);
		client.path("/certs/delete_identity");
		Response resp = client.post(alias);
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

		// Confirm it has been deleted
		client = newClient();
		client.path("/certs/list_identities");
		List<Cert> keys = client.get(new GenericType<List<Cert>>() { });
		boolean contained = false;
		for (Cert k : keys) {
			contained |= alias.equals(k.alias);
		}
		assertFalse(contained);
	}

	@Test
	public void deleteCerts() {
		// Get list of certs
		WebClient client = newClient();
		client.accept(MediaType.APPLICATION_JSON);
		client.path("/certs/list_certs");
		List<Cert> certs = client.get(new GenericType<List<Cert>>() {
		});
		assumeFalse(certs.isEmpty());

		// Choose a cert and delete it
		client = newClient();
		String alias = certs.get(0).alias;
		client.header("Content-type", MediaType.APPLICATION_JSON);
		client.path("/certs/delete_cert");
		Response resp = client.post(alias);
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

		// Confirm it has been deleted
		client = newClient();
		client.path("/certs/list_certs");
		List<Cert> keys = client.get(new GenericType<List<Cert>>() {
		});
		boolean contained = false;
		for (Cert k : keys) {
			contained |= alias.equals(k.alias);
		}
		assertFalse(contained);
	}
}
