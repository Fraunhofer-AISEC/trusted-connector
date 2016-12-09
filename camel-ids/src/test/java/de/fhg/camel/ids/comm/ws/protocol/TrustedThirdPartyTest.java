package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.sql.SQLException;

import org.apache.camel.test.AvailablePortFinder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.Database;
import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.attestation.REST;
import de.fhg.aisec.ids.attestation.RemoteAttestationServer;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	//private static Server server;
	private static URI ttpUri;
	private static Server server;
	static String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	static TrustedThirdParty ttp;
	static PcrValue[] one;
	static PcrValue[] two;
	static RemoteAttestationServer ratServer;

	@BeforeClass
	public static void initRepo() throws Exception {
		ratServer = new RemoteAttestationServer("127.0.0.1" , "check", AvailablePortFinder.getNextAvailable());
        try {
        	ratServer.start();
        } finally {
        	ratServer.stop();
        }
        ttpUri = ratServer.getURI();
        
    	one = new PcrValue[10];
    	two = new PcrValue[23];
    	for(int i = 0; i<10;i++) {
    		one[i] = new PcrValue(i, zero);
    	}
    	for(int i = 0; i<23;i++) {
    		two[i] = new PcrValue(i, zero);
    	}
    	ttp = new TrustedThirdParty(ttpUri);        
	}
	
	@AfterClass
	public static void stopRepo() throws Exception {
		ratServer.stop();
		ratServer.destroy();
	}
	

    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"success\":false,\"signature\":\"\",\"values\":[{\"order\":0,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":1,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":2,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":3,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":4,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":5,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":6,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":7,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":8,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":9,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"}]}";
    	System.out.println(ttp.toJsonString(new PcrMessage(one)));
    	assertTrue(ttp.toJsonString(new PcrMessage(one)).equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	assertTrue(ttp.pcrValuesCorrect(one, "abc"));
    }
}
