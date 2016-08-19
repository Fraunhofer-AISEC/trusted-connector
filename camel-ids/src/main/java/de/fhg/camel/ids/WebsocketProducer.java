package de.fhg.camel.ids;

import java.io.IOException;
import java.util.Collection;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;

public class WebsocketProducer extends DefaultProducer {
    

    private WebsocketStore store;

    public WebsocketProducer(Endpoint endpoint) {
        super(endpoint);
    }

    public WebsocketProducer(Endpoint endpoint, WebsocketStore store) {
        super(endpoint);
        this.store = store;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        String message = in.getBody(String.class);
        
        if (isSendToAllSet(in)) {
            sendToAll(store, message);
        }
        else {
            // look for connection key and get Websocket
            String connectionKey = in.getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
            if (connectionKey != null) {
                DefaultWebsocket websocket = store.get(connectionKey);
                sendMessage(websocket, message);
            } 
            else {
                throw new IllegalArgumentException("Failed to send message to single connection; connetion key not set.");
            }
        }
    }
    
    boolean isSendToAllSet(Message in) {
        // header may be null; have to be careful here
        Object value = in.getHeader(WebsocketConstants.SEND_TO_ALL);
        return value == null ? false : (Boolean) value;
    }
    
    void sendToAll(WebsocketStore store, String message) throws Exception {
        Collection<DefaultWebsocket> websockets = store.getAll();
        Exception exception = null;
        for (DefaultWebsocket websocket : websockets) {
            try {
                sendMessage(websocket, message);
            } 
            catch (Exception e) {
                if (exception == null) {
                    exception = new Exception("Failed to deliver message to one or more recipients.", e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    void sendMessage(DefaultWebsocket websocket, String message) throws IOException {
        // in case there is web socket and socket connection is open - send message
        if (websocket != null && websocket.getConnection().isOpen()) {
            websocket.getConnection().getRemote().sendString(message);
        }
    }
}
