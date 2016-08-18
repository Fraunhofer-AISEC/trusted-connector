/**
 * 
 */
package org.apache.camel.component.websocket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WebsocketComponentServletTest {
    
    private static final String PROTOCOL       = "ws";
    private static final String MESSAGE        = "message";
    private static final String CONNECTION_KEY = "random-connection-key";
    
    @Mock
    private WebsocketConsumer consumer;

    @Mock
    private WebsocketStore store;
    
    @Mock
    private HttpServletRequest request;

    private WebsocketComponentServlet websocketComponentServlet;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        websocketComponentServlet = new WebsocketComponentServlet(store);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponentServlet#getConsumer()}.
     */
    @Test
    public void testGetConsumer() {
        assertNull(websocketComponentServlet.getConsumer());
        websocketComponentServlet.setConsumer(consumer);
        assertEquals(consumer, websocketComponentServlet.getConsumer());
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponentServlet#setConsumer(org.apache.camel.component.websocket.WebsocketConsumer)}.
     */
    @Test
    public void testSetConsumer() {
        testGetConsumer();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponentServlet#doWebSocketConnect(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void testDoWebSocketConnect() {
        websocketComponentServlet.setConsumer(consumer);
        WebSocket webSocket = websocketComponentServlet.doWebSocketConnect(request, PROTOCOL);
        assertNotNull(webSocket);
        assertEquals(DefaultWebsocket.class, webSocket.getClass());
        DefaultWebsocket defaultWebsocket = (DefaultWebsocket) webSocket;
        defaultWebsocket.setConnectionKey(CONNECTION_KEY);
        defaultWebsocket.onMessage(MESSAGE);
        InOrder inOrder = inOrder(consumer, store, request);
        inOrder.verify(consumer, times(1)).sendExchange(CONNECTION_KEY, MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponentServlet#doWebSocketConnect(javax.servlet.http.HttpServletRequest, java.lang.String)}.
     */
    @Test
    public void testDoWebSocketConnectConsumerIsNull() {
        WebSocket webSocket = websocketComponentServlet.doWebSocketConnect(request, PROTOCOL);
        assertNotNull(webSocket);
        assertEquals(DefaultWebsocket.class, webSocket.getClass());
        DefaultWebsocket defaultWebsocket = (DefaultWebsocket) webSocket;
        defaultWebsocket.setConnectionKey(CONNECTION_KEY);
        defaultWebsocket.onMessage(MESSAGE);
        InOrder inOrder = inOrder(consumer, store, request);
        inOrder.verifyNoMoreInteractions();
    }
}
