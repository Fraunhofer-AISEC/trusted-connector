package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.attestation.REST;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	private static Server server;
	private static URI ttpUri;
	Pcr one;
	Pcr two;
	String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	TrustedThirdParty ttp;
	PcrMessage msg;

	@BeforeClass
	public static void initRepo() throws Exception {
		/*server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(31330); // let connector pick an unused port #
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        
        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", REST.class.getCanonicalName());

        // Start Server
        server.start();

        int port = connector.getLocalPort();*/
        ttpUri = new URI(String.format("http://127.0.0.1:%d", 7331));
	}
	
	@AfterClass
	public static void stopRepo() throws Exception {
		server.stop();
		server.destroy();
	}
	
    @Before
    public void initTest() {
    	PcrValue[] one = new PcrValue[10];
    	PcrValue[] two = new PcrValue[23];
    	for(int i = 0; i<10;i++) {
    		one[i] = new PcrValue(i, zero);
    	}
    	for(int i = 0; i<23;i++) {
    		two[i] = new PcrValue(i, zero);
    	}
    	msg = new PcrMessage(two);
    	this.ttp = new TrustedThirdParty(one, ttpUri);
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"success\":false,\"signature\":\"\",\"msg\":{\"bitField0_\":3,\"messageCase_\":10,\"message_\":{\"bitField0_\":63,\"atype_\":0,\"qualifyingData_\":\"aaa\",\"halg_\":\"halg\",\"quoted_\":\"quoted\",\"signature_\":\"sign\",\"pcrValues_\":[{\"bitField0_\":3,\"number_\":0,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":1,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}],\"certificateUri_\":\"public\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},\"type_\":8,\"id_\":1,\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}}";
    	assertTrue(this.ttp.jsonToString("").equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	msg.setNonce("abc");
    	msg.setSignature("bcd");
    	msg.setSuccess(false);
    	Gson gson = new Gson();
    	PcrMessage pcrResult = this.ttp.readResponse(gson.toJson(msg));
    	assertTrue(pcrResult.getNonce().equals("abc"));
    	assertTrue(pcrResult.getSignature().equals("bcd"));
    	
    }
}
