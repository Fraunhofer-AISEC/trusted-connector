package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.net.URI;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.attestation.REST;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationProviderHandler;
import de.fraunhofer.aisec.tpm2j.tpm.TPM_ALG_ID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteAttestationTest {
	
	private static RemoteAttestationConsumerHandler consumer;
	private static RemoteAttestationProviderHandler provider;
	private long id = 1234567;
	private IdsAttestationType aType = IdsAttestationType.BASIC;
	private TPM_ALG_ID.ALG_ID hAlg = TPM_ALG_ID.ALG_ID.TPM_ALG_SHA256;
	
	private ConnectorMessage startMsg = Idscp.ConnectorMessage.newBuilder().setType(ConnectorMessage.Type.RAT_START).setId(id).build();
	private Event startEvent = new Event(startMsg.getType(), startMsg.toString(), startMsg);
	
	private static ConnectorMessage msg1;
	private static ConnectorMessage msg2;
	private static ConnectorMessage msg3;
	private static ConnectorMessage msg4;
	private static ConnectorMessage msg5;
	private static ConnectorMessage msg6;
	private static ConnectorMessage msg7;
	private static URI ttpUri;
	private static Server server;
	
	@BeforeClass
	public static void setupSocket() throws Exception {
		FSM fsm1 = new FSM();
		FSM fsm2 = new FSM();
		/*
		server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // let connector pick an unused port #
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        
        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", REST.class.getCanonicalName());

        // Start Server
        server.start();

        int port = connector.getLocalPort();*/
        ttpUri = new URI(String.format("http://127.0.0.1:%d", 7331));
		consumer = new RemoteAttestationConsumerHandler(fsm1, IdsAttestationType.BASIC, ttpUri);
		provider = new RemoteAttestationProviderHandler(fsm2, IdsAttestationType.BASIC, ttpUri);
	}
	
	@AfterClass
	public static void stopRepo() throws Exception {
		server.stop();
		server.destroy();
	}	

    @Test
    public void test1() throws Exception {
    	msg1 = ConnectorMessage.parseFrom(consumer.enterRatRequest(startEvent).toByteString());
    	assertTrue(msg1.getId() == id + 1);
    	assertTrue(msg1.getType().equals(ConnectorMessage.Type.RAT_REQUEST));
    	System.out.println(msg1.toString());
    }
    
    @Test
    public void test2() throws Exception {
    	msg2 = ConnectorMessage.parseFrom(provider.sendTPM2Ddata(new Event(msg1.getType(), msg1.toString(), msg1)).toByteString()); 
    	assertTrue(msg2.getId() == id + 2);
    	assertTrue(msg2.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    	assertTrue(msg2.getAttestationResponse().getAtype().equals(aType));
    	assertTrue(msg2.getAttestationResponse().getHalg().equals(hAlg.name()));
    	assertTrue(msg2.getAttestationResponse().getPcrValuesCount() == 11);
    	System.out.println(msg2.toString());
    }
   
    @Test
    public void test3() throws Exception {
    	msg3 = ConnectorMessage.parseFrom(consumer.sendTPM2Ddata(new Event(msg2.getType(), msg2.toString(), msg2)).toByteString());
    	assertTrue(msg3.getId() == id + 3);
    	assertTrue(msg3.getType().equals(ConnectorMessage.Type.RAT_RESPONSE));
    	assertTrue(msg3.getAttestationResponse().getAtype().equals(aType));
    	assertTrue(msg3.getAttestationResponse().getHalg().equals(hAlg.name()));
    	assertTrue(msg3.getAttestationResponse().getPcrValuesCount() == 11);
    	System.out.println(msg3.toString());
    }

    @Test
    public void test4() throws Exception {
    	msg4 = ConnectorMessage.parseFrom(provider.sendResult(new Event(msg3.getType(), msg3.toString(), msg3)).toByteString());
    	assertTrue(msg4.getId() == id + 4);
    	assertTrue(msg4.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    	assertTrue(msg4.getAttestationResponse().getAtype().equals(aType));
    	System.out.println(msg4.toString());
    }
    
    @Test
    public void test5() throws Exception {
    	msg5 = ConnectorMessage.parseFrom(consumer.sendResult(new Event(msg4.getType(), msg4.toString(), msg4)).toByteString());
    	assertTrue(msg5.getId() == id + 5);
    	assertTrue(msg5.getType().equals(ConnectorMessage.Type.RAT_RESULT));
    	assertTrue(msg5.getAttestationResponse().getAtype().equals(aType));
    	System.out.println(msg5.toString());
    }

    @Test
    public void test6() throws Exception {
    	msg6 = ConnectorMessage.parseFrom(provider.leaveRatRequest(new Event(msg5.getType(), msg5.toString(), msg5)).toByteString());
    	assertTrue(msg6.getId() == id + 6);
    	assertTrue(msg6.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    	assertTrue(msg6.getAttestationResponse().getAtype().equals(aType));
    	System.out.println(msg6.toString());
    }

    @Test
    public void test7() throws Exception {
    	msg7 = ConnectorMessage.parseFrom(consumer.leaveRatRequest(new Event(msg6.getType(), msg6.toString(), msg6)).toByteString());
    	assertTrue(msg7.getId() == id + 7);
    	assertTrue(msg7.getType().equals(ConnectorMessage.Type.RAT_LEAVE));
    	assertTrue(msg7.getAttestationResponse().getAtype().equals(aType));
    	System.out.println(msg7.toString());
    }
}
