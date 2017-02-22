package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataProviderHandler;

//import de.fhg.ids.docker.Docker;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MetadataIT {
	
	private static MetadataConsumerHandler consumer;
	private static MetadataProviderHandler provider;
	private static Logger LOG = LoggerFactory.getLogger(MetadataIT.class);
	private long id = 87654321;

	private ConnectorMessage msg0 = ConnectorMessage.newBuilder().setType(ConnectorMessage.Type.RAT_LEAVE).setId(id).build();
	
	private static ConnectorMessage msg1;
	private static ConnectorMessage msg2;
	private static ConnectorMessage msg3;
	//private static Docker docker;
	
	@BeforeClass
	public static void initRepo() throws URISyntaxException {
        consumer = new MetadataConsumerHandler();
		provider = new MetadataProviderHandler();
		//docker = new Docker();
	}
	
    @Test
    public void test1() throws Exception {
    	msg1 = ConnectorMessage.parseFrom(consumer.request(new Event(msg0.getType(), msg0.toString(), msg0)).toByteString());
    	LOG.debug("msg1: " + msg1.toString());
    	assertTrue(msg0.getId() == id);
    	assertTrue(msg1.getId() == id + 1);
    	assertTrue(msg1.getType().equals(ConnectorMessage.Type.META_REQUEST));
    	assertTrue(msg1.getMetadataExchange().getKeyCount() == 1);
    	assertTrue(msg1.getMetadataExchange().getKeyList().get(0).equals("labels"));
    }
    
    @Test
    public void test2() throws Exception {
    	msg2 = ConnectorMessage.parseFrom(provider.request(new Event(msg1.getType(), msg1.toString(), msg1)).toByteString());
    	LOG.debug(msg2.toString());
    	assertTrue(msg2.getId() == id + 2);
    	assertTrue(msg2.getType().equals(ConnectorMessage.Type.META_REQUEST));
    	assertTrue(msg2.getMetadataExchange().getKeyCount() == 1);
    	assertTrue(msg2.getMetadataExchange().getKeyList().get(0).equals("labels"));
    	assertTrue(msg2.getMetadataExchange().getValueCount() == 1);
    }    
    
    @Test
    public void test3() throws Exception {
    	msg3 = ConnectorMessage.parseFrom(consumer.response(new Event(msg2.getType(), msg2.toString(), msg2)).toByteString()); 
    	LOG.debug(msg3.toString());
    	assertTrue(msg3.getType().equals(ConnectorMessage.Type.META_RESPONSE));
    	assertTrue(msg3.getId() == id + 3);
    	assertTrue(msg3.getMetadataExchange().getValueCount() == 1);
    }

    /*
    @Test
    public void test4() throws Exception {
    	docker.connectClient();
    	String list = docker.getJsonMetaData().toString();
    	String ids = docker.getJsonIDs().toString();
    	List<String> idsList = docker.getIDs();
    	List<String> names = docker.getRunningNames();
    	String labels = docker.getJsonLabel(idsList.get(0)).toString();
    	LOG.debug(list);
    	LOG.debug(ids.toString());
    	LOG.debug(names.toString());
    	LOG.debug(labels);
    }
    */
}