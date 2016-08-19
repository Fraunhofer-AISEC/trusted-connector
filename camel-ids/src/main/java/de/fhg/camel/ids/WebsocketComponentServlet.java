package de.fhg.camel.ids;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebsocketComponentServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    private static final long serialVersionUID = 207837507742337364L;

    private WebsocketStore store;
    private WebsocketConsumer consumer;

    public WebsocketComponentServlet(WebsocketStore store) {
        this.store = store;
    }

    /**
     * @return the consumer
     */
    public WebsocketConsumer getConsumer() {
    	return consumer;
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(WebsocketConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        getServletContext().getNamedDispatcher("default").forward(request,
                response);
    }

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(new MyWebsocketCreator(store, consumer));		
	}

}
