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

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.rat.PcrMessage;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	Pcr one;
	Pcr two;
	String pcrValue = "0000000000000000000000000000000000000000000000000000000000000000";
	ConnectorMessage msg;
	TrustedThirdParty ttp;

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
    	this.ttp = new TrustedThirdParty(this.msg);
    }
    
    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "";
    	assertTrue(this.ttp.jsonToString("myFunkyFreshNonce").equals(result));
    }

    @Test
    public void testReadResponse() throws Exception {
    	String request = "";
    	PcrMessage pcrResult = this.ttp.readResponse(request, "http://127.0.0.1:7331/");
    	assertTrue(pcrResult.getNonce().equals("myFunkyFreshNonce"));
    	assertTrue(pcrResult.getSignature().equals(""));
    	
    }
}
