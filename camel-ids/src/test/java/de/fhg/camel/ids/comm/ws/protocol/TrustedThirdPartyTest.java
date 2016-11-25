package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.PcrMessage;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationClientHandler;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	Pcr[] values;
	Pcr one;
	Pcr two;
	String pcrValue = "0000000000000000000000000000000000000000000000000000000000000000";
	TrustedThirdParty ttp;
	private static String dockerName = "ttp";
    
	/*
	@BeforeClass
    public static void initTTP() throws InterruptedException, IOException {
		Docker.initDocker(dockerName, 4);
    }
	
	@AfterClass
    public static void kilTTP() throws Exception {
		Docker.killDocker(dockerName, 4);
    }
	*/
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
