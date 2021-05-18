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
import de.fhg.aisec.ids.webconsole.api.data.User
import io.swagger.annotations.Api
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.util.Calendar
import javax.crypto.KeyGenerator
import javax.security.auth.callback.Callback
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.login.Configuration
import javax.security.auth.login.LoginContext
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
class UserApi {
    companion object {
        private val LOG = LoggerFactory.getLogger(UserApi::class.java)
        var key: Key? = null

        init {
            try {
                key = KeyGenerator.getInstance("HmacSHA256").generateKey()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
        }
    }

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
        return try {
            // Authenticate the user using the credentials provided
            if (!authenticate(user.username, user.password)) {
                return Response.status(Response.Status.UNAUTHORIZED).build()
            }
            // Issue a token for the user
            val token = issueToken(user.username)
            Response.ok().entity(mapOf("token" to token)).build()
        } catch (e: Throwable) {
            e.printStackTrace()
            Response.status(Response.Status.UNAUTHORIZED).build()
        }
    }

    private fun issueToken(username: String?): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = cal.timeInMillis + 86400000
        val tomorrow = cal.time
        return JWT.create()
            .withClaim("user", username)
            .withExpiresAt(tomorrow)
            .withIssuer("ids-connector")
            .sign(Algorithm.HMAC256(key!!.encoded))
    }

    /**
     * Login with JaaS. We use the default realm "karaf". When using default PropertiesLoginModule, users are
     * configured in karaf-assembly/src/main/resources/etc/users.properties. Other modules such as OAuth, LDAP, JDBC
     * can be configured as needed without changing this code here.
     */
    @Throws(LoginException::class)
    private fun authenticate(user: String?, password: String?): Boolean {
        if (Configuration.getConfiguration().getAppConfigurationEntry("karaf") == null) {
            LOG.warn(
                "No LoginModules configured for karaf. This is okay if running as unit test. " +
                    "If this message appears in Karaf container, make sure that JAAS is available."
            )
            return "ids" == user && "ids" == password
        }
        val ctx = LoginContext(
            "karaf"
        ) { callbacks: Array<Callback> ->
            for (cb in callbacks) {
                if (cb is PasswordCallback) {
                    cb.password = password?.toCharArray()
                }
                if (cb is NameCallback) {
                    cb.name = user
                }
            }
        }
        ctx.login()
        return true
    }

    @POST
    @Path("/setPassword")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun setPassword(password: String) {
        // find user
        // val u:User = null

        // set password
        // u.password = password

        // save user
    }

    @POST
    @Path("/addUser")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addUser(user: User) {
    }

    @POST
    @Path("/removeUser")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun removeUser(username: String) {
    }
}
