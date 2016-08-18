package org.apache.camel.component.websocket;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

public class WebsocketConsumer extends DefaultConsumer {

    public WebsocketConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    public void sendExchange(String connectionKey, String message) {
        Exchange exchange = this.getEndpoint().createExchange();

        // set header and body
        exchange.getOut().setHeader(WebsocketConstants.CONNECTION_KEY,
                connectionKey);
        exchange.getOut().setBody(message);

        // send exchange
        try {
            this.getProcessor().process(exchange);
        } catch (Exception e) {
            if (exchange.getException() != null) {
                this.getExceptionHandler()
                        .handleException(
                                String.format("Error processing exchange for websocket consumer on message '%s'.", message), 
                                exchange, exchange.getException());
            }
        }
    }

}
