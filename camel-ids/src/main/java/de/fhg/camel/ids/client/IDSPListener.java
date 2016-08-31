package de.fhg.camel.ids.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.ws.DefaultWebSocketListener;
import com.ning.http.client.ws.WebSocket;

import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
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

	@Override
    public void onOpen(WebSocket websocket) {
        LOG.debug("Websocket opened");
        
        // create Finite State Machine for IDS protocol
        fsm = new ProtocolMachine().initIDSConsumerProtocol(websocket);
        
        // start the protocol with the first message
        fsm.feedEvent(new Event("start rat", null));
    }

    @Override
    public void onClose(WebSocket websocket) {
        LOG.debug("websocket closed - reconnecting");
        fsm.reset();
    }

    @Override
    public void onError(Throwable t) {
        LOG.debug("websocket on error", t);
        fsm.reset();
    }

    @Override
    public void onMessage(byte[] message) {
    	fsm.feedEvent(new Event("TODO:EXTRACT KEY", new String(message)));
    }

    @Override
    public void onMessage(String message) {
    	fsm.feedEvent(new Event("TODO:EXTRACT KEY", message));
    }	
}
