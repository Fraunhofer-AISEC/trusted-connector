package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.apache.camel.test.AvailablePortFinder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.Database;
import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.attestation.REST;
import de.fhg.aisec.ids.attestation.RemoteAttestationServer;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TrustedThirdPartyTest {
    
	//private static Server server;
	private static URI ttpUri;
	private static Logger LOG = LoggerFactory.getLogger(TrustedThirdPartyTest.class);
	private static Server server;
	static String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	static TrustedThirdParty ttp;
	static PcrValue[] one;
	static PcrValue[] two;
	static RemoteAttestationServer ratServer;
	
	@BeforeClass
	public static void initRepo() throws Exception {
		ratServer = new RemoteAttestationServer("127.0.0.1", "configurations/check", AvailablePortFinder.getNextAvailable());
		ratServer.start();
		ttp = new TrustedThirdParty(ratServer.getURI());
    	one = new PcrValue[10];
    	for(int i = 0; i<10;i++) {
    		one[i] = new PcrValue(i, zero);
    	}
	}
	
	@AfterClass
	public static void stopRepo() throws Exception {
		ratServer.stop();
		ratServer.destroy();
	}

    @Test
    public void test1() throws Exception {
    	String result = "{\"success\":false,\"signature\":\"\",\"values\":[{\"order\":0,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":1,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":2,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":3,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":4,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":5,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":6,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":7,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":8,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":9,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"}]}";
    	PcrMessage test = new PcrMessage(one);
    	assertTrue(ttp.toJsonString(test).equals(result));
    }

    @Test
    @Ignore // TTP has to be completed
    public void test2() throws Exception {
    	assertTrue(ttp.pcrValuesCorrect(one, "abc"));
    }
}
