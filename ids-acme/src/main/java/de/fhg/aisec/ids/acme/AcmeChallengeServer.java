package de.fhg.aisec.ids.acme;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcmeChallengeServer {
    public static final String TEXT_PLAIN = "text/plain";
    public static final Pattern ACME_REGEX = Pattern.compile("^.*?/\\.well-known/acme-challenge/(.+)$");
    private static NanoHTTPD server = null;
    private static final Logger LOG = LoggerFactory.getLogger(AcmeChallengeServer.class);

    public static void startServer(final AcmeClient acmeClient) throws IOException {
        server = new NanoHTTPD(5002) {
            @Override
            public Response serve(IHTTPSession session) {
                Matcher tokenMatcher = ACME_REGEX.matcher(session.getUri());
                if (!tokenMatcher.matches()) {
                    LOG.error("Received invalid ACME challenge: " + session.getUri());
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, TEXT_PLAIN, null);
                }
                String token = tokenMatcher.group(1);
                LOG.info("Received ACME challenge: " + token);
                String response = acmeClient.getChallengeAuthorization(token);
                if (response == null) {
                    LOG.warn("ACME challenge is unknown");
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, TEXT_PLAIN, null);
                } else {
                    LOG.warn("ACME challenge response: " + response);
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, TEXT_PLAIN, response);
                }
            }
        };
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        LOG.debug("NanoHTTPD started");
    }

    public static void stopServer() {
        server.stop();
        LOG.debug("NanoHTTPD stopped");
    }

}
