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

import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.attestation.REST;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	private static Server server;
	private static URI serverUri;
	Pcr one;
	Pcr two;
	String pcrValue = "0000000000000000000000000000000000000000000000000000000000000000";
	ConnectorMessage msg;
	TrustedThirdParty ttp;

	@BeforeClass
	public static void initRepo() throws Exception {
		server = new Server();
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
        serverUri = new URI("http://127.0.0.1:31330/check");
	}
	
	@AfterClass
	public static void stopRepo() throws Exception {
		server.stop();
		server.destroy();
	}
	
    @Before
    public void initTest() {
    	this.one = Pcr
    			.newBuilder()
    			.setNumber(0)
    			.setValue(pcrValue)
    			.build();
    	this.two = Pcr
    			.newBuilder()
    			.setNumber(1)
    			.setValue(pcrValue)
    			.build();
    	this.msg = ConnectorMessage
				.newBuilder()
				.setId(1)
				.setType(ConnectorMessage.Type.RAT_RESPONSE)
				.setAttestationResponse(
						AttestationResponse
						.newBuilder()
						.setAtype(IdsAttestationType.BASIC)
						.setQualifyingData("aaa")
						.setHalg("halg")
						.setQuoted("quoted")
						.setSignature("sign")
						.addAllPcrValues(Arrays.asList(new Pcr[] {this.one, this.two}))
						.setCertificateUri("public")
						.build()
						)
				.build();
    	this.ttp = new TrustedThirdParty(this.msg, serverUri);
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"success\":false,\"signature\":\"\",\"msg\":{\"bitField0_\":3,\"messageCase_\":10,\"message_\":{\"bitField0_\":63,\"atype_\":0,\"qualifyingData_\":\"aaa\",\"halg_\":\"halg\",\"quoted_\":\"quoted\",\"signature_\":\"sign\",\"pcrValues_\":[{\"bitField0_\":3,\"number_\":0,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":1,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}],\"certificateUri_\":\"public\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},\"type_\":8,\"id_\":1,\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}}";
    	assertTrue(this.ttp.jsonToString("").equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	// Issue request to server
    	String request = "sers";
    	PcrMessage pcrResult = this.ttp.readResponse(request);
    	assertTrue(pcrResult.getNonce().equals("myFunkyFreshNonce"));
    	assertTrue(pcrResult.getSignature().equals(""));
    	
    }
}
