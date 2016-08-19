/**
 * 
 */
package de.fhg.camel.ids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.fhg.camel.ids.DefaultWebsocket;
import de.fhg.camel.ids.WebsocketConsumer;
import de.fhg.camel.ids.WebsocketStore;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultWebsocketTest {
    
    private static final int CLOSE_CODE = -1;
    private static final String MESSAGE = "message";
    private static final String CONNECTION_KEY = "random-connection-key";
    
    @Mock
    private Session connection;

    @Mock
    private WebsocketConsumer consumer;

    @Mock
    private WebsocketStore store;

    private DefaultWebsocket defaultWebsocket;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        defaultWebsocket = new DefaultWebsocket(store, consumer);
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#onClose(int, java.lang.String)}.
     */
    @Test
    public void testOnClose() {
        defaultWebsocket.onWebSocketClose(CLOSE_CODE, MESSAGE);
        InOrder inOrder = inOrder(connection, consumer, store);
        inOrder.verify(store, times(1)).remove(defaultWebsocket);
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#onOpen(org.eclipse.jetty.websocket.WebSocket.Connection)}.
     */
    @Test
    public void testOnOpen() {
        defaultWebsocket.onWebSocketConnect(connection);
        InOrder inOrder = inOrder(connection, consumer, store);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        inOrder.verify(store, times(1)).add(keyCaptor.capture(), eq(defaultWebsocket));
        inOrder.verifyNoMoreInteractions();
        assertEquals(defaultWebsocket.getConnectionKey(), keyCaptor.getValue());
        assertEquals(connection, defaultWebsocket.getConnection());
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#onMessage(java.lang.String)}.
     */
    @Test
    public void testOnMessage() {
        defaultWebsocket.setConnectionKey(CONNECTION_KEY);
        defaultWebsocket.onWebSocketText(MESSAGE);
        InOrder inOrder = inOrder(connection, consumer, store);
        inOrder.verify(consumer, times(1)).sendExchange(CONNECTION_KEY, MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#onMessage(java.lang.String)}.
     */
    @Test
    public void testOnMessageWithNullConsumer() {
        defaultWebsocket = new DefaultWebsocket(store, null);
        defaultWebsocket.setConnectionKey(CONNECTION_KEY);
        defaultWebsocket.onWebSocketText(MESSAGE);
        InOrder inOrder = inOrder(connection, consumer, store);
        inOrder.verify(consumer, times(0)).sendExchange(CONNECTION_KEY, MESSAGE);
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#getConnection()}.
     */
    @Test
    public void testGetConnection() {
        assertNull(defaultWebsocket.getConnection());
        defaultWebsocket.onWebSocketConnect(connection);
        assertEquals(connection, defaultWebsocket.getConnection());
        defaultWebsocket.setConnection(null);
        assertNull(defaultWebsocket.getConnection());
        defaultWebsocket.setConnection(connection);
        assertEquals(connection, defaultWebsocket.getConnection());
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#setConnection(org.eclipse.jetty.websocket.WebSocket.Connection)}.
     */
    @Test
    public void testSetConnection() {
        testGetConnection();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#getConnectionKey()}.
     */
    @Test
    public void testGetConnectionKey() {
        assertNull(defaultWebsocket.getConnectionKey());
        defaultWebsocket.onWebSocketConnect(connection);
        assertNotNull(defaultWebsocket.getConnectionKey());
        defaultWebsocket.setConnectionKey(CONNECTION_KEY);
        assertEquals(CONNECTION_KEY, defaultWebsocket.getConnectionKey());
        defaultWebsocket.setConnectionKey(null);
        assertNull(defaultWebsocket.getConnectionKey());
    }

    /**
     * Test method for {@link de.fhg.camel.ids.DefaultWebsocket#setConnectionKey(java.lang.String)}.
     */
    @Test
    public void testSetConnectionKey() {
        testGetConnectionKey();
    }
}
