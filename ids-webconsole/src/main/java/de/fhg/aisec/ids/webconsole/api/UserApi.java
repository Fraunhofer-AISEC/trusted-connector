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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import de.fhg.aisec.ids.webconsole.api.data.User;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/user")
@Api(value = "User Authentication")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UserApi {
  private static final Logger LOG = LoggerFactory.getLogger(UserApi.class);
  static Key key;

  static {
    try {
      key = KeyGenerator.getInstance("HmacSHA256").generateKey();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
  }

  /**
   * Given a correct username/password, this method returns a JWT token that is valid for one day.
   *
   * @param user Username/password.
   * @return A JSON object of the form <code>{ "token" : <jwt token> }</code> if successful, 401
   *     UNAUTHORIZED if not.
   */
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response authenticateUser(User user) {
    try {
      // Authenticate the user using the credentials provided
      if (!authenticate(user.username, user.password)) {
        return Response.status(UNAUTHORIZED).build();
      }

      // Issue a token for the user
      String token = issueToken(user.username);

      // Return the token on the response
      Map<String, String> result = new HashMap<>();
      result.put("token", token);
      return Response.ok().entity(result).build();
    } catch (Throwable e) {
      e.printStackTrace();
      return Response.status(UNAUTHORIZED).build();
    }
  }

  private String issueToken(String username) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(cal.getTimeInMillis() + 86_400_000);
    Date tomorrow = cal.getTime();

    return JWT.create()
        .withClaim("user", username)
        .withExpiresAt(tomorrow)
        .withIssuer("ids-connector")
        .sign(Algorithm.HMAC256(UserApi.key.getEncoded()));
  }

  private boolean authenticate(String user, String password) throws LoginException {
    /* Login with JaaS. We use the default realm "karaf". When using default PropertiesLoginModule, users are
    configured in karaf-assembly/src/main/resources/etc/users.properties. Other modules such as OAuth, LDAP, JDBC
    can be configured as needed without changing this code here.
    */
    if (Configuration.getConfiguration().getAppConfigurationEntry("karaf") == null) {
      LOG.warn("No LoginModules configured for karaf. This is okay if running as unit test. " +
              "If this message appears in Karaf container, make sure that JAAS is available.");
      return "ids".equals(user) && "ids".equals(password);
    }
    LoginContext ctx =
        new LoginContext(
            "karaf",
                callbacks -> {
                  for (Callback cb : callbacks) {
                    if (cb instanceof PasswordCallback) {
                      ((PasswordCallback) cb).setPassword(password.toCharArray());
                    }
                    if (cb instanceof NameCallback) {
                      ((NameCallback) cb).setName(user);
                    }
                  }
                });
    ctx.login();
    return true;
  }
}
