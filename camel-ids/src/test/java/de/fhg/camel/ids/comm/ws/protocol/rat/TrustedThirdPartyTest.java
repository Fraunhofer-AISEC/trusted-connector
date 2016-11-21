package de.fhg.camel.ids.comm.ws.protocol.rat;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.aisec.ids.messages.Idscp.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.PcrMessage;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	Pcr[] values;
	Pcr one;
	Pcr two;
	TrustedThirdParty ttp;
	static Process docker;
	private static String dockerName = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dmock:latest";
	
    @Before
    public void initTest() {
    	this.one = Pcr
    			.newBuilder()
    			.setNumber(0)
    			.setValue("0000000000000000000000000000000000000000000000000000000000000000")
    			.build();
    	this.two = Pcr
    			.newBuilder()
    			.setNumber(1)
    			.setValue("0000000000000000000000000000000000000000000000000000000000000000")
    			.build();
    	this.values = new Pcr[] {this.one, this.two};
    	this.ttp = new TrustedThirdParty(this.values);
    }
    
    @BeforeClass
    public  static void initMockServer() throws InterruptedException, IOException {
    	// build a docker imagess
    	new ProcessBuilder("docker", "build", "-t", dockerName, "mock").start();
    	// then start the docker image as ttp for the server
    	new ProcessBuilder("docker", "run", "--rm", "-i", "--name", "ttp", "-p", "127.0.0.1:7331:29663", dockerName, "/tpm2d/ttp.py").start();
    }
    
    @AfterClass
    public static void teardownMockServer() throws Exception {
		new ProcessBuilder("docker", "stop", "-t", "0", "ttp").start();
    	new ProcessBuilder("docker", "rm", "-f", "ttp").start();
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"nonce\":\"myFunkyFreshNonce\",\"values\":{\"0\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"1\":\"0000000000000000000000000000000000000000000000000000000000000000\"},\"success\":false,\"signature\":\"\"}";
    	assertTrue(this.ttp.jsonToString("myFunkyFreshNonce").equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	String request = "{\"nonce\":\"myFunkyFreshNonce\",\"values\":{\"0\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"1\":\"0000000000000000000000000000000000000000000000000000000000000000\"},\"success\":false,\"signature\":\"\"}";
    	PcrMessage pcrResult = this.ttp.readResponse(request, "http://localhost:7331/");
    	assertTrue(pcrResult.getNonce().equals("myFunkyFreshNonce"));
    	assertTrue(pcrResult.getSignature().equals(""));
    	for (Map.Entry<Integer, String> entry : pcrResult.getValues().entrySet()) {
    	    assertTrue(entry.getValue().equals(this.one.getValue()));
    	}
    }
}
