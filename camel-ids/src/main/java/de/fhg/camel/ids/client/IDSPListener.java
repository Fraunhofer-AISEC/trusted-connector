package de.fhg.camel.ids.client;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.asynchttpclient.ws.DefaultWebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage.Type;
import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

/**
 * Handles messages for the IDS protocol.
 * 
 * Messages from and to the web socket are connected to the FSM implementing the actual protocol.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class IDSPListener extends DefaultWebSocketListener {
    private Logger LOG = LoggerFactory.getLogger(IDSPListener.class);
    private FSM fsm;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isFinishedCond = lock.newCondition();
    private final ConnectorMessage emptyMsg = Idscp.ConnectorMessage.newBuilder().build();

	@Override
    public void onOpen(WebSocket websocket) {
        LOG.debug("Websocket opened");
        
        // create Finite State Machine for IDS protocol
        fsm = new ProtocolMachine().initIDSConsumerProtocol(websocket);
        
        // start the protocol with the first message
        fsm.feedEvent(new Event(ConnectorMessage.Type.RAT_START, "", emptyMsg));
    }

    @Override
    public void onClose(WebSocket websocket) {
        LOG.debug("websocket closed - reconnecting");
        fsm.reset();
    }

    @Override
    public void onError(Throwable t) {
        LOG.debug("websocket on error", t);
        if (fsm!=null) {
        	fsm.reset();
        }
    }

    @Override
    public void onMessage(byte[] message) {
    	try {
    		lock.lockInterruptibly();
    		try {
    			ConnectorMessage msg = ConnectorMessage.parseFrom(message);
    			fsm.feedEvent(new Event(msg.getType(), new String(message), msg));
    		} catch (InvalidProtocolBufferException e) {
    			e.printStackTrace();
    		}

    		if (fsm.getState().equals(ProtocolState.IDSCP_END.id())) {
	    		isFinishedCond.signalAll();
	    	}

		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
    }

    @Override
    public void onMessage(String message) {
    	onMessage(message.getBytes());
    }
    
    public ReentrantLock semaphore() {
    	return lock;
    }

    public Condition isFinished() {
    	return isFinishedCond;
    }
}
