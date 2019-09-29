package de.fhg.aisec.ids.webconsole.api;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import de.fhg.aisec.ids.webconsole.api.data.User;
import io.swagger.annotations.Api;

import javax.crypto.KeyGenerator;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
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
@Api(value = "User")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class UserApi {

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
     * @return A JSON object of the form <code>{ "token" : <jwt token> }</code> if successful, 401 UNAUTHORIZED if not.
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
        cal.add(Calendar.DAY_OF_MONTH, 1);
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
        LoginContext ctx = new LoginContext("karaf", new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) {
                for (Callback cb : callbacks) {
                    if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword(password.toCharArray());
                    }
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName(user);
                    }
                }
            }
        });
        ctx.login();
        return true;
    }
}