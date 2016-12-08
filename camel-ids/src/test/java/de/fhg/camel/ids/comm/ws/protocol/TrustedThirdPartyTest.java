package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	//private static Server server;
	private static URI ttpUri;
	String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	TrustedThirdParty ttp;
	PcrMessage msg;
	PcrValue[] one;
	PcrValue[] two;

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
	
	/*
	@AfterClass
	public static void stopRepo() throws Exception {
		server.stop();
		server.destroy();
	}
	*/
	
    @Before
    public void initTest() {
    	one = new PcrValue[10];
    	two = new PcrValue[23];
    	for(int i = 0; i<10;i++) {
    		one[i] = new PcrValue(i, zero);
    	}
    	for(int i = 0; i<23;i++) {
    		two[i] = new PcrValue(i, zero);
    	}
    	msg = new PcrMessage(one);
    	this.ttp = new TrustedThirdParty(ttpUri);
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"success\":false,\"signature\":\"\",\"values\":[{\"order\":0,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":1,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":2,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":3,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":4,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":5,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":6,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":7,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":8,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":9,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"}]}";
    	this.ttp.setValues(one);
    	System.out.println(this.ttp.jsonToString("ads"));
    	assertTrue(this.ttp.jsonToString("").equals(result));
    }

    @Test
    @Ignore
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
