package de.fhg.ids.comm.client;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.asynchttpclient.ws.DefaultWebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class IdspClientSocket extends DefaultWebSocketListener {
    private Logger LOG = LoggerFactory.getLogger(IdspClientSocket.class);
    private FSM fsm;
    private int attestationType = 0;
    private int attestationMask = 0;
    private ProtocolMachine machine;
    private boolean ratSuccess = false;
    private SSLContextParameters params;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isFinishedCond = lock.newCondition();
    private final ConnectorMessage startMsg = Idscp.ConnectorMessage
										    		.newBuilder()
										    		.setType(ConnectorMessage.Type.RAT_START)
										    		.setId(new java.util.Random().nextLong())
										    		.build();


	
	public IdspClientSocket(int attestationType, int attestationMask, SSLContextParameters params) {
		this.attestationType = attestationType;
		this.attestationMask = attestationMask;
		this.params = params;
	}

	@Override
    public void onOpen(WebSocket websocket) {
        LOG.debug("Websocket opened");
        IdsAttestationType type;
        switch(this.attestationType) {
	    	case 0:            
	    		type = IdsAttestationType.BASIC;
	    		break;
	    	case 1:
	    		type = IdsAttestationType.ALL;
	    		break;
	    	case 2:
	    		type = IdsAttestationType.ADVANCED;
	    		break;
	    	case 3:
	    		type = IdsAttestationType.ZERO;
	    		break;
	    	default:
	    		type = IdsAttestationType.BASIC;
	    		break;	    		
        }
        // create Finite State Machine for IDS protocol
        machine = new ProtocolMachine();
        fsm = machine.initIDSConsumerProtocol(websocket, type, this.attestationMask, this.params);
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
    			LOG.error(e.getMessage(), e);
    		}
    		if (fsm.getState().equals(ProtocolState.IDSCP_END.id())) {
	    		isFinishedCond.signalAll();
	    	}
    	} catch (InterruptedException e) {
			LOG.warn(e.getMessage());
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
	
    //get the result of the remote attestation
	public boolean isAttestationSuccessful() {
		return machine.getIDSCPConsumerSuccess();
	}

    //get the result of the remote attestation
	public AttestationResult getAttestationResult() {
		if (machine.getAttestationType()==IdsAttestationType.ZERO) {
			return AttestationResult.SKIPPED;
		} else {
			if (machine.getIDSCPConsumerSuccess()) {
				return AttestationResult.SUCCESS;
			} else {
				return AttestationResult.FAILED;
			}
		}
	}
}
