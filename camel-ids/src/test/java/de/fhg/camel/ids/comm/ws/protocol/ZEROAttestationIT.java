package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

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
public class ZEROAttestationIT {
	
	private static RemoteAttestationConsumerHandler consumer;
	private static RemoteAttestationProviderHandler provider;
	private static Logger LOG = LoggerFactory.getLogger(ZEROAttestationIT.class);
	private long id = 87654321;
	private static IdsAttestationType aType = IdsAttestationType.ZERO;
	
	private ConnectorMessage msg0 = Idscp.ConnectorMessage.newBuilder().setType(ConnectorMessage.Type.RAT_START).setId(id).build();
	
	private static ConnectorMessage msg1;
	private static ConnectorMessage msg2;
	private static URI ttpUri;
	private static Server server;
	private static FSM fsm1;
	private static FSM fsm2;


	@BeforeClass
	public static void initRepo() throws URISyntaxException {
		SSLContextParameters params = defineClientSSLContextParameters();
        consumer = new RemoteAttestationConsumerHandler(new FSM(), aType, 0, new URI(""), "", params);
		provider = new RemoteAttestationProviderHandler(new FSM(), aType, 0, new URI(""), "", params);
	}
	
    @Test
    public void test1() throws Exception {
    	msg1 = ConnectorMessage.parseFrom(consumer.sendNoAttestation(new Event(msg0.getType(), msg0.toString(), msg0)).toByteString());
    	LOG.debug(msg1.toString());
    	assertTrue(msg1.getId() == id + 1);
    	assertTrue(msg1.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
    }
    
    @Test
    public void test2() throws Exception {
    	msg2 = ConnectorMessage.parseFrom(provider.sendNoAttestation(new Event(msg1.getType(), msg1.toString(), msg1)).toByteString());
    	LOG.debug(msg2.toString());
    	assertTrue(msg2.getId() == id + 2);
    	assertTrue(msg2.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    	
    }  
    
    public static SSLContextParameters defineClientSSLContextParameters() {
    	SSLContextParameters scp = new SSLContextParameters(); 
        return scp;
    }
}