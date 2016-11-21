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
	String pcrValue = "0000000000000000000000000000000000000000000000000000000000000000";
	TrustedThirdParty ttp;
	private static String dockerName = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dmock:latest";
	private static Process docker;
	
	
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
    	this.values = new Pcr[] {this.one, this.two};
    	this.ttp = new TrustedThirdParty(this.values);
    }
    
    @BeforeClass
    public  static void initMockServer() throws InterruptedException, IOException {
    	dockerStop();   	
    	// build a docker imagess
    	new ProcessBuilder("docker", "build", "-t", dockerName, "mock").start().waitFor();
    	// then start the docker image as ttp for the server
    	docker = new ProcessBuilder("docker", "run", "-i", "--name", "ttp", "-p", "127.0.0.1:7331:29663", dockerName, "/tpm2d/ttp.py").start();
    }
    
    @AfterClass
    public static void teardownMockServer() throws Exception {
    	dockerStop();
    	docker.destroy();
    }
    
    private static void dockerStop() throws InterruptedException, IOException {
		new ProcessBuilder("docker", "stop", "-t", "0", "ttp").start().waitFor();
    	new ProcessBuilder("docker", "rm", "-f", "ttp").start().waitFor();
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"nonce\":\"myFunkyFreshNonce\",\"values\":{\"0\":\""+pcrValue+"\",\"1\":\""+pcrValue+"\"},\"success\":false,\"signature\":\"\"}";
    	assertTrue(this.ttp.jsonToString("myFunkyFreshNonce").equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	String request = "{\"nonce\":\"myFunkyFreshNonce\",\"values\":{\"0\":\""+pcrValue+"\",\"1\":\""+pcrValue+"\"},\"success\":false,\"signature\":\"\"}";
    	PcrMessage pcrResult = this.ttp.readResponse(request, "http://127.0.0.1:7331/");
    	assertTrue(pcrResult.getNonce().equals("myFunkyFreshNonce"));
    	assertTrue(pcrResult.getSignature().equals(""));
    	for (Map.Entry<Integer, String> entry : pcrResult.getValues().entrySet()) {
    	    assertTrue(entry.getValue().equals(this.one.getValue()));
    	}
    }
}
