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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.api.data.User
import io.swagger.annotations.Api
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Calendar
import javax.security.auth.login.LoginException
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("/user")
@Api(value = "User Authentication")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserApi(@Autowired private val settings: Settings) {
    /**
     * Given a correct username/password, this method returns a JWT token that is valid for one day.
     *
     * @param user Username/password.
     * @return A JSON object of the form `{ "token" : <jwt token> }</jwt>` if successful, 401 UNAUTHORIZED if not.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun authenticateUser(user: User): Response {
        if (user.username.isNullOrBlank() || user.password.isNullOrBlank()) {
            LOG.error("Username or password blank, please provide valid login credentials!")
        } else {
            try {
                // Authenticate the user using the credentials provided
                if (!authenticate(user.username!!, user.password!!)) {
                    return Response.status(Response.Status.UNAUTHORIZED).build()
                }
                // Issue a token for the user
                val token = issueToken(user.username)
                return Response.ok().entity(mapOf("token" to token)).build()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).build()
    }

    private fun issueToken(username: String?): String {
        val tomorrow = Calendar.getInstance().apply { timeInMillis += 86400000 }.time
        return JWT.create()
            .withClaim("user", username)
            .withExpiresAt(tomorrow)
            .withIssuer("ids-connector")
            .sign(Algorithm.HMAC256(key))
    }

    /**
     * Login with JaaS. We use the default realm "karaf". When using default PropertiesLoginModule, users are
     * configured in karaf-assembly/src/main/resources/etc/users.properties. Other modules such as OAuth, LDAP, JDBC
     * can be configured as needed without changing this code here.
     */
    @Throws(LoginException::class)
    private fun authenticate(username: String, password: String): Boolean {
        return if (settings.isUserStoreEmpty()) {
            LOG.warn("WARNING: User store is empty! This is insecure! Please create an admin user via the REST API!")
            username == "ids" && password == "ids"
        } else {
            val expectedHash = settings.getUserHash(username) ?: randomHash
            val loginOk = argonEncoder.matches(password, expectedHash)
            // Upgrade hashing difficulty if necessary
            if (loginOk && argonEncoder.upgradeEncoding(expectedHash)) {
                settings.saveUser(username, argonEncoder.encode(password))
            }
            loginOk
        }
    }

    @POST
    @Path("/saveUser")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addUser(user: User) {
        if (user.username.isNullOrBlank() || user.password.isNullOrBlank()) {
            LOG.error("Username or password blank, please provide valid credentials!")
        } else {
            settings.saveUser(user.username!!, argonEncoder.encode(user.password))
        }
    }

    @POST
    @Path("/removeUser")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun removeUser(username: String) {
        settings.removeUser(username)
    }

    /*
    @GET
    @Path("list_users")
    @ApiOperation(
            value = "List installed certificates from the private key store.",
            notes = "Certificates in this list refer to private keys that can be used as identities by the connector."
    )
    @Produces(
            MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun listUsers(): List<User> {
        val keystoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        return getKeystoreEntries(keystoreFile)
    }
    */

    companion object {
        private val LOG = LoggerFactory.getLogger(UserApi::class.java)
        val argonEncoder = Argon2PasswordEncoder(16, 32, 1, 1 shl 13, 10)
        var key = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val randomHash: String = argonEncoder.encode(String(key, StandardCharsets.UTF_8))
    }
}
