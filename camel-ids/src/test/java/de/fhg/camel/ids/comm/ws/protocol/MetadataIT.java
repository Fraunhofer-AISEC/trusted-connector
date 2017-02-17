package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.InspectContainerResponse;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

import de.fhg.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataProviderHandler;
import de.fhg.ids.docker.Docker;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MetadataIT {
	
	private static MetadataConsumerHandler consumer;
	private static MetadataProviderHandler provider;
	private static Logger LOG = LoggerFactory.getLogger(MetadataIT.class);
	private long id = 87654321;

	private ConnectorMessage msg0 = ConnectorMessage.newBuilder().setType(ConnectorMessage.Type.META_START).setId(id).build();
	
	private static ConnectorMessage msg1;
	private static ConnectorMessage msg2;
	private static ConnectorMessage msg3;
	private static ConnectorMessage msg4;
	private static FSM fsm1;
	private static FSM fsm2;
	private static Docker docker;
	
	@BeforeClass
	public static void initRepo() throws URISyntaxException {
		fsm1 = new FSM();
		fsm2 = new FSM();
        consumer = new MetadataConsumerHandler();
		provider = new MetadataProviderHandler();
		docker = new Docker();
	}
	
    @Test
    public void test1() throws Exception {
    	msg1 = ConnectorMessage.parseFrom(consumer.request(new Event(msg0.getType(), msg0.toString(), msg0)).toByteString());
    	LOG.debug(msg1.toString());
    	assertTrue(msg0.getId() == id);
    	assertTrue(msg1.getId() == id + 1);
    }
    
    @Test
    public void test2() throws Exception {
    	msg2 = ConnectorMessage.parseFrom(provider.request(new Event(msg1.getType(), msg1.toString(), msg1)).toByteString());
    	LOG.debug(msg2.toString());
    	assertTrue(msg2.getId() == id + 2);
    }    
    
    @Test
    public void test3() throws Exception {
    	msg3 = ConnectorMessage.parseFrom(consumer.response(new Event(msg2.getType(), msg2.toString(), msg2)).toByteString()); 
    	LOG.debug(msg3.toString());
    	assertTrue(msg3.getId() == id + 3);
    }

    @Test
    public void test4() throws Exception {
    	msg4 = ConnectorMessage.parseFrom(provider.response(new Event(msg3.getType(), msg3.toString(), msg3)).toByteString());
    	LOG.debug(msg4.toString());
    	assertTrue(msg4.getId() == id + 4);
    }

    @Test
    public void test5() throws Exception {
    	docker.connectClient();
    	List<InspectContainerResponse> list = docker.getMetaData();
    	LOG.debug(list.get(0).getId());
    }
}