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
import de.fhg.aisec.ids.webconsole.ApiController
import de.fhg.aisec.ids.webconsole.api.data.PasswordChangeRequest
import de.fhg.aisec.ids.webconsole.api.data.User
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Calendar
import javax.security.auth.login.LoginException
import javax.ws.rs.core.MediaType

@ApiController
@RequestMapping("/user")
@Api(value = "User Authentication")
class UserApi(
    @Autowired private val settings: Settings
) {
    /**
     * Given a correct username/password, this method returns a JWT token that is valid for one day.
     *
     * @param user Username/password.
     * @return A JWT token as plain text, if successful, 401 UNAUTHORIZED if not.
     */
    @PostMapping("/login", produces = [MediaType.TEXT_PLAIN], consumes = [MediaType.APPLICATION_JSON])
    fun authenticateUser(
        @RequestBody user: User
    ): String {
        if (user.username.isBlank() || user.password.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Username or password blank, please provide valid login credentials!"
            )
        } else {
            try {
                // Authenticate the user using the credentials provided
                if (!authenticate(user.username, user.password)) {
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
                }
                // Issue a token for the user
                return issueToken(user.username)
            } catch (e: Throwable) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, e.message, e)
            }
        }
    }

    private fun issueToken(username: String?): String {
        val tomorrow = Calendar.getInstance().apply { timeInMillis += 86400000 }.time
        return JWT.create()
            .withClaim("user", username)
            .withExpiresAt(tomorrow)
            .withIssuer("ids-connector")
            .sign(Algorithm.HMAC256(key))
    }

    @Throws(LoginException::class)
    private fun authenticate(
        username: String,
        password: String
    ): Boolean {
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

    @PostMapping("/saveUser", consumes = [MediaType.APPLICATION_JSON])
    fun addUser(
        @RequestBody user: User
    ) {
        if (user.username.isBlank() || user.password.isBlank()) {
            LOG.error("Username or password blank, please provide valid credentials!")
        } else {
            settings.saveUser(user.username, argonEncoder.encode(user.password))
        }
    }

    @PostMapping("/setPassword", consumes = [MediaType.APPLICATION_JSON])
    fun setPassword(
        @RequestBody change: PasswordChangeRequest
    ) {
        if (change.username.isBlank() || change.oldPassword.isBlank() || change.newPassword.isBlank()) {
            LOG.error("Username or password blank, please provide valid credentials!")
        } else if (
            argonEncoder.matches(
                change.oldPassword,
                (settings.getUserHash(change.username) ?: randomHash)
            )
        ) {
            settings.saveUser(change.username, argonEncoder.encode(change.newPassword))
        } else {
            LOG.error("Old password is wrong")
        }
    }

    @DeleteMapping(
        "/removeUser/{user}",
        consumes = [MediaType.APPLICATION_JSON],
        produces = [MediaType.APPLICATION_JSON]
    )
    fun removeUser(
        @PathVariable("user") username: String
    ) = settings.removeUser(username)

    @GetMapping("list_user_names", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Lists user names", responseContainer = "List")
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "List of user names",
            response = String::class,
            responseContainer = "List"
        )
    )
    fun listUsersNames(): List<String> = settings.getUsers().keys.toList()

    companion object {
        private val LOG = LoggerFactory.getLogger(UserApi::class.java)
        val argonEncoder = Argon2PasswordEncoder(16, 32, 1, 1 shl 13, 10)
        var key = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val randomHash: String = argonEncoder.encode(String(key, StandardCharsets.UTF_8))
    }
}
