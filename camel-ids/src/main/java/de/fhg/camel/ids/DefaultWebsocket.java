package de.fhg.camel.ids;

import java.io.Serializable;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;


public class DefaultWebsocket extends WebSocketAdapter implements Serializable {

    private static final long serialVersionUID = -575701599776801400L;
    private Session connection;
    private String connectionKey;

    private transient WebsocketStore store;
    private transient WebsocketConsumer consumer;

    public DefaultWebsocket(WebsocketStore store, WebsocketConsumer consumer) {
        this.store = store;
        this.consumer = consumer;
    }

    @Override
    public void onWebSocketClose(int closeCode, String message) {
        store.remove(this);
    }

    @Override
    public void onWebSocketConnect(Session connection) {
        this.connection = connection;
        this.connectionKey = UUID.randomUUID().toString();
        store.add(this.connectionKey, this);
    }

    @Override
    public void onWebSocketText(String message) {
        if (this.consumer != null) {
            this.consumer.sendExchange(this.connectionKey, message);
        }
        // consumer is not set, this is produce only websocket
        // TODO - 06.06.2011, LK - deliver exchange to dead letter channel
    }

    // getters and setters
    public Session getConnection() {
        return connection;
    }

    public void setConnection(Session connection) {
        this.connection = connection;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }

    public void setStore(WebsocketStore store) {
        this.store = store;
    }

    public void setConsumer(WebsocketConsumer consumer) {
        this.consumer = consumer;
    }
}
