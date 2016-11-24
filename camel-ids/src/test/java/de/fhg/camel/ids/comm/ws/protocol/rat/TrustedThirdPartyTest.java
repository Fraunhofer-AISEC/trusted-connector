package de.fhg.camel.ids.comm.ws.protocol.rat;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.PcrMessage;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	Pcr[] values;
	Pcr one;
	Pcr two;
	String pcrValue = "0000000000000000000000000000000000000000000000000000000000000000";
	TrustedThirdParty ttp;
    private static String DOCKER_CLI ="docker";
    private static String DOCKER_IMAGE = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dsim:latest";
    private static String SOCKET = "control.sock";
    private static String SOCKET_PATH = "tpm2sim/socket/" + SOCKET;
	private static File socketFile;
    
	@BeforeClass
    public static void initSimServer() throws InterruptedException, IOException {
    	socketFile = new File(SOCKET_PATH);
		String folder = socketFile.getAbsolutePath().substring(0, socketFile.getAbsolutePath().length() - SOCKET.length());
		// pull the image
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "build", "-t", DOCKER_IMAGE, "./tpm2sim/")).start().waitFor(660, TimeUnit.SECONDS);
    	// then start the docker image
		TrustedThirdPartyTest.kill("ttp");
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "run", "--name", "ttp", "-v", folder +":/data/cml/communication/tpm2d/", "-p", "127.0.0.1:7331:29663", DOCKER_IMAGE, "/tpm2d/start.sh")).start().waitFor(2, TimeUnit.SECONDS);
    }
	
	@AfterClass
    public static void teardownSimServer() throws Exception {
		TrustedThirdPartyTest.kill("ttp");
		socketFile.delete();
    }
	
	private static void kill(String id) throws InterruptedException, IOException {
		// pull the image
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "stop", id)).start().waitFor(5, TimeUnit.SECONDS);
    	// pull the image
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "rm", id)).start().waitFor(5, TimeUnit.SECONDS);
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
    	this.values = new Pcr[] {this.one, this.two};
    	this.ttp = new TrustedThirdParty(this.values);
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
