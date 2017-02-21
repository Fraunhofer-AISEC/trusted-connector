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
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
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
    private int attestationType = 0;
    private int attestationMask = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isFinishedCond = lock.newCondition();
    private final ConnectorMessage startMsg = Idscp.ConnectorMessage
    		.newBuilder()
    		.setType(ConnectorMessage.Type.RAT_START)
    		.setId(new java.util.Random().nextLong())
    		.build(); 

	public IDSPListener(int attestationType, int attestationMask) {
		this.attestationType = attestationType;
		this.attestationMask = attestationMask;
	}

	@Override
    public void onOpen(WebSocket websocket) {
        LOG.debug("Websocket opened");
        IdsAttestationType type;
        switch(this.attestationType) {
	    	case 1:            
	    		type = IdsAttestationType.BASIC;
	    		break;
	    	case 2:
	    		type = IdsAttestationType.ADVANCED;
	    		break;
	    	case 3:
	    		type = IdsAttestationType.ALL;
	    		break;
	    	default:
	    		type = IdsAttestationType.ZERO;
	    		break;
        }
        // create Finite State Machine for IDS protocol
        fsm = new ProtocolMachine().initIDSConsumerProtocol(websocket, type, this.attestationMask);
        // start the protocol with the first message
        fsm.feedEvent(new Event(startMsg.getType(), startMsg.toString(), startMsg));
        
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
