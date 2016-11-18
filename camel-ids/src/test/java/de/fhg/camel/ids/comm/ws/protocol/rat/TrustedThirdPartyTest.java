package de.fhg.camel.ids.comm.ws.protocol.rat;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fhg.aisec.ids.messages.Idscp.Pcr;
import de.fhg.ids.comm.ws.protocol.rat.TrustedThirdParty;

public class TrustedThirdPartyTest {
    
	Pcr[] values;
	TrustedThirdParty ttp;
	
    @Before
    public void initTest() {
    	Pcr one = Pcr
    			.newBuilder()
    			.setNumber(0)
    			.setValue("0000000000000000000000000000000000000000000000000000000000000000")
    			.build();
    	Pcr two = Pcr
    			.newBuilder()
    			.setNumber(1)
    			.setValue("0000000000000000000000000000000000000000000000000000000000000000")
    			.build();
    	this.values = new Pcr[] {one, two};
    	this.ttp = new TrustedThirdParty(this.values);
    }
    
    @After
    public void teardownTest() {

    }

    @Test
    public void testObjToJsonString() throws Exception {
    	String result = "{\"nonce\":\"myFunkyFreshNonce\",\"values\":{\"0\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"1\":\"0000000000000000000000000000000000000000000000000000000000000000\"}}";
    	assertTrue(this.ttp.jsonToString("myFunkyFreshNonce").equals(result));
    }
}
