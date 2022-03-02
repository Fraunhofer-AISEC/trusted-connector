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
package de.fhg.aisec.ids.webconsole.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import de.fhg.aisec.ids.api.settings.ConnectorConfig
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.api.data.Cert
import de.fhg.aisec.ids.webconsole.api.data.Identity
import de.fhg.aisec.ids.webconsole.api.data.User
import org.apache.cxf.endpoint.Server
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean
import org.apache.cxf.jaxrs.client.WebClient
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider
import org.apache.cxf.transport.local.LocalConduit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.Mockito
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.String

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RestApiTests : Assertions() {
    private fun newClient(token: String?): WebClient {
        // Client uses Jackson for JSON mapping
        val jackson = JacksonJsonProvider()
        jackson.setMapper(ObjectMapper())
        val client = WebClient.create(ENDPOINT_ADDRESS, listOf(jackson))
        WebClient.getConfig(client).requestContext[LocalConduit.DIRECT_DISPATCH] = true
        token?.let {
            client.header("Authorization", "Bearer: $it")
        }
        return client
    }

    @BeforeEach
    fun before() {
        val certApi = CertApi(settings)
        val idSpec = Identity()
        idSpec.c = "c"
        idSpec.cn = "common name"
        idSpec.l = "location"
        idSpec.s = "subject"
        certApi.createIdentity(idSpec)
    }

    @Test
    @Order(1)
    fun testListCerts() {
        val token = login()
        val client = newClient(token!!)
        client.accept(MediaType.APPLICATION_JSON)
        client.path("/certs/list_certs")
        val certs: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        assertNotNull(certs)
        assertTrue(certs.isNotEmpty())
    }

    @Test
    @Order(1)
    fun testListIdentities() {
        val token = login()
        val client = newClient(token!!)
        client.accept(MediaType.APPLICATION_JSON)
        client.path("/certs/list_identities")
        val keys: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        assertNotNull(keys)
        assertTrue(keys.isNotEmpty())
    }

    @Test
    @Order(2)
    fun testCreateIdentity() {
        val token = login()
        val client = newClient(token!!)
        val idSpec = Identity()
        idSpec.c = "c"
        idSpec.cn = "common name"
        idSpec.l = "location"
        idSpec.s = "subject"
        client.accept(MediaType.APPLICATION_JSON)
        client.header("Content-type", MediaType.APPLICATION_JSON)
        client.path("/certs/create_identity")
        val alias = client.post(idSpec, String::class.java)
        assertTrue(alias.length > 5)
    }

    @Test
    @Order(3)
    fun testDeleteIdentity() {
        val token = login()

        // Get list of identities
        var client = newClient(token!!)
        client.accept(MediaType.APPLICATION_JSON)
        client.path("/certs/list_identities")
        val certs: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        assertFalse(certs.isEmpty())

        // Choose an identity and delete it
        client = newClient(token)
        val alias = certs[0].alias
        client.header("Content-type", MediaType.APPLICATION_JSON)
        client.path("/certs/delete_identity")
        val resp = client.post(alias)
        assertEquals(Response.Status.OK.statusCode.toLong(), resp.status.toLong())

        // Confirm it has been deleted
        client = newClient(token)
        client.path("/certs/list_identities")
        val keys: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        var contained = false
        for (k in keys) {
            contained = contained or (alias == k.alias)
        }
        assertFalse(contained)
    }

    @Disabled("Needs Fix, non-critical")
    @Test
    fun deleteCerts() {
        val token = login()

        // Get list of certs
        var client = newClient(token!!)
        client.accept(MediaType.APPLICATION_JSON)
        client.path("/certs/list_certs")
        val certs: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        assertFalse(certs.isEmpty())

        // Choose a cert and delete it
        client = newClient(token)
        val alias = certs[0].alias
        client.header("Content-type", MediaType.APPLICATION_JSON)
        client.path("/certs/delete_cert")
        val resp = client.post(alias)
        assertEquals(Response.Status.OK.statusCode.toLong(), resp.status.toLong())

        // Confirm it has been deleted
        client = newClient(token)
        client.path("/certs/list_certs")
        val keys: List<Cert> = client.get(object : GenericType<List<Cert>>() {})
        var contained = false
        for (k in keys) {
            contained = contained or (alias == k.alias)
        }
        assertFalse(contained)
    }

    // Access a protected endpoint
    @Test
    fun getMetricsTest() {
        val token = login()

        // Access a protected endpoint
        val c = newClient(token!!)
        c.accept(MediaType.APPLICATION_JSON)
        c.path("/metric/get")
        val metrics: Map<String, String> =
            c.get(object : GenericType<Map<String, String>>() {})
        assertFalse(metrics.isEmpty())
    }

    /**
     * Retrieves a fresh JWT from server.
     *
     * @return The generated authentication token
     */
    private fun login(): String? {
        val c = newClient(null)
        c.path("/user/login")
        c.accept(MediaType.APPLICATION_JSON)
        c.header("Content-type", MediaType.APPLICATION_JSON)
        val u = User()
        u.username = "ids"
        u.password = "ids"
        val result: Map<String, String> =
            c.post(u, object : GenericType<Map<String, String>>() {})
        val token = result["token"]
        c.header("Authorization", "Bearer: $token")
        return token
    }

    companion object {
        private const val ENDPOINT_ADDRESS = "local://testserver"
        private lateinit var server: Server
        private val settings = Mockito.mock(
            Settings::class.java
        )

        @BeforeAll
        @JvmStatic
        fun initialize() {
            val connectorConfig = ConnectorConfig()
            Mockito.`when`(settings.connectorConfig).thenReturn(connectorConfig)
            Mockito.`when`(settings.isUserStoreEmpty()).thenReturn(true)
            startServer()
        }

        /** Starts a test server. Note that REST endpoints must be registered manually here.  */
        private fun startServer() {
            val sf = JAXRSServerFactoryBean()
            sf.setResourceClasses(CertApi::class.java)
            sf.setResourceClasses(MetricAPI::class.java)
            sf.setResourceClasses(UserApi::class.java)

            // Server uses Jackson for JSON mapping
            val providers: MutableList<Any> = ArrayList()
            val jackson = JacksonJsonProvider()
            jackson.setMapper(ObjectMapper())
            providers.add(jackson)
            providers.add(JWTRestAPIFilter())
            // add custom providers if any
            sf.providers = providers
            sf.setResourceProvider(CertApi::class.java, SingletonResourceProvider(CertApi(settings), true))
            sf.setResourceProvider(MetricAPI::class.java, SingletonResourceProvider(MetricAPI(), true))
            sf.setResourceProvider(UserApi::class.java, SingletonResourceProvider(UserApi(settings), true))
            sf.address = ENDPOINT_ADDRESS
            server = sf.create()
        }

        @AfterAll
        @JvmStatic
        fun destroy() {
            server.run {
                stop()
                destroy()
            }
        }
    }
}
