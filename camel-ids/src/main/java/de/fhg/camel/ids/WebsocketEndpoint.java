package de.fhg.camel.ids;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class WebsocketEndpoint extends DefaultEndpoint {

    private WebsocketStore store = new MemoryWebsocketStore();
    private String remaining;

    public WebsocketEndpoint() {

    }

    public WebsocketEndpoint (String uri, WebsocketComponent component, String remaining) {
        super(uri, component);
        this.remaining = remaining;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {

        // init consumer
        WebsocketConsumer consumer = new WebsocketConsumer(this, processor);

        // register servlet
        ((WebsocketComponent) super.getComponent()).addServlet(this.store, consumer, this.remaining);

        return consumer;
    }

    @Override
    public Producer createProducer() throws Exception {

        // register servlet without consumer
        ((WebsocketComponent) super.getComponent()).addServlet(this.store, null, this.remaining);

        return new WebsocketProducer(this, this.store);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    // TODO --> implement store factory
}
