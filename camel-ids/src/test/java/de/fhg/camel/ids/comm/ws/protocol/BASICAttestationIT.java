/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.util.jsse.ClientAuthentication;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SSLContextServerParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Ignore;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationProviderHandler;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// BASIC test
public class BASICAttestationIT {
	
	private static RemoteAttestationConsumerHandler consumer;
	private static RemoteAttestationProviderHandler provider;
	private static Logger LOG = LoggerFactory.getLogger(BASICAttestationIT.class);
	private long id = 87654321;
	private static IdsAttestationType aType = IdsAttestationType.BASIC;
	private TPM_ALG_ID.ALG_ID hAlg = TPM_ALG_ID.ALG_ID.TPM_ALG_SHA256;
	
	private ConnectorMessage msg0 = Idscp.ConnectorMessage.newBuilder().setType(ConnectorMessage.Type.RAT_START).setId(id).build();
	
	private static ConnectorMessage msg1;
	private static ConnectorMessage msg2;
	private static ConnectorMessage msg3;
	private static ConnectorMessage msg4;
	private static ConnectorMessage msg5;
	private static ConnectorMessage msg6;
	private static ConnectorMessage msg7;
	private static ConnectorMessage msg8;
	private static String PWD = "password";
	private static String ratRepoUri = "https://127.0.0.1:31337/configurations/check";

	@BeforeClass
	public static void initRepo() throws URISyntaxException {
		SSLContextParameters sslContextParameters = defineClientSSLContextParameters();
        consumer = new RemoteAttestationConsumerHandler(new FSM(), aType, 0, new URI(ratRepoUri), "socket/control.sock", sslContextParameters);
		provider = new RemoteAttestationProviderHandler(new FSM(), aType, 0, new URI(ratRepoUri), "socket/control.sock", sslContextParameters);
	}
	
    @Test
    public void test1() throws Exception {
    	msg1 = ConnectorMessage.parseFrom(consumer.enterRatRequest(new Event(msg0.getType(), msg0.toString(), msg0)).toByteString());
    	LOG.debug(msg1.toString());
    	assertTrue(msg1.getId() == id + 1);
    	assertTrue(msg1.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
    	
    }
    
    @Test
    public void test2() throws Exception {
    	msg2 = ConnectorMessage.parseFrom(provider.enterRatRequest(new Event(msg1.getType(), msg1.toString(), msg1)).toByteString());
    	LOG.debug(msg2.toString());
    	assertTrue(msg2.getId() == id + 2);
    	assertTrue(msg2.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
    }    
    
    @Test
    public void test3() throws Exception {
    	msg3 = ConnectorMessage.parseFrom(consumer.sendTPM2Ddata(new Event(msg2.getType(), msg2.toString(), msg2)).toByteString()); 
    	LOG.debug(msg3.toString());
    	assertTrue(msg3.getId() == id + 3);
    	assertTrue(msg3.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    	assertTrue(msg3.getAttestationResponse().getAtype().equals(aType));
    	assertTrue(msg3.getAttestationResponse().getHalg().equals(hAlg.name()));
    	assertTrue(msg3.getAttestationResponse().getPcrValuesCount() == 11);
    }

    @Test
    public void test4() throws Exception {
    	msg4 = ConnectorMessage.parseFrom(provider.sendTPM2Ddata(new Event(msg3.getType(), msg3.toString(), msg3)).toByteString());
    	LOG.debug(msg4.toString());
    	assertTrue(msg4.getId() == id + 4);
    	assertTrue(msg4.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    	assertTrue(msg4.getAttestationResponse().getAtype().equals(aType));
    	assertTrue(msg4.getAttestationResponse().getHalg().equals(hAlg.name()));
    	assertTrue(msg4.getAttestationResponse().getPcrValuesCount() == 11);
    }

    @Test
    public void test5() throws Exception {
    	msg5 = ConnectorMessage.parseFrom(consumer.sendResult(new Event(msg4.getType(), msg4.toString(), msg4)).toByteString());
    	LOG.debug(msg5.toString());
    	assertTrue(msg5.getId() == id + 5);
    	assertTrue(msg5.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    	assertTrue(msg5.getAttestationResult().getResult());
    	assertTrue(msg5.getAttestationResult().getAtype().equals(aType));
    }

    @Test
    public void test6() throws Exception {
    	msg6 = ConnectorMessage.parseFrom(provider.sendResult(new Event(msg5.getType(), msg5.toString(), msg5)).toByteString());
    	LOG.debug(msg6.toString());
    	assertTrue(msg6.getId() == id + 6);
    	assertTrue(msg6.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    	assertTrue(msg6.getAttestationResult().getResult());
    	assertTrue(msg6.getAttestationResult().getAtype().equals(aType));
    }

    @Test
    public void test7() throws Exception {
    	msg7 = ConnectorMessage.parseFrom(consumer.leaveRatRequest(new Event(msg6.getType(), msg6.toString(), msg6)).toByteString());
    	LOG.debug(msg7.toString());
    	assertTrue(msg7.getId() == id + 7);
    	assertTrue(msg7.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    	assertTrue(msg7.getAttestationLeave().getAtype().equals(aType));
    }
    
    @Test
    public void test8() throws Exception {
    	msg8 = ConnectorMessage.parseFrom(provider.leaveRatRequest(new Event(msg7.getType(), msg7.toString(), msg7)).toByteString());
    	LOG.debug(msg8.toString());
    	assertTrue(msg8.getId() == id + 8);
    	assertTrue(msg8.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    	assertTrue(msg8.getAttestationLeave().getAtype().equals(aType));
    }
    
    public static SSLContextParameters defineClientSSLContextParameters() {
		String PWD = "password";
		String KEYSTORE = "jsse/client-keystore.jks";
		String TRUSTSTORE = "jsse/client-truststore.jks";
		
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(Thread.currentThread().getContextClassLoader().getResource(KEYSTORE).toString());
        ksp.setPassword(PWD);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(PWD);
        kmp.setKeyStore(ksp);

        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(Thread.currentThread().getContextClassLoader().getResource(TRUSTSTORE).toString());
        tsp.setPassword(PWD);
        LOG.debug("------------------------------------------------------------------------------");
        LOG.debug(tsp.toString());
        LOG.debug("------------------------------------------------------------------------------");
        
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);

        SSLContextServerParameters scsp = new SSLContextServerParameters();
        //scsp.setClientAuthentication(ClientAuthentication.REQUIRE.name());
        scsp.setClientAuthentication(ClientAuthentication.NONE.name());

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        sslContextParameters.setTrustManagers(tmp);
        sslContextParameters.setServerParameters(scsp);
        
        return sslContextParameters;
    }
}
