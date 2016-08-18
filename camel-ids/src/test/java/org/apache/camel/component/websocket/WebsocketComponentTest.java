/**
 * 
 */
package org.apache.camel.component.websocket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WebsocketComponentTest {

    private static final String PATH_ONE = "foo";
    private static final String PATH_TWO = "bar";
    private static final String PATH_SPEC_ONE = "/" + PATH_ONE + "/*";
    private static final String PATH_SPEC_TWO = "/" + PATH_TWO + "/*";

    @Mock
    private WebsocketConsumer consumer;

    @Mock
    private WebsocketStore store;

    @Mock
    private WebsocketComponentServlet servlet;

    @Mock
    private Map<String, WebsocketComponentServlet> servlets;

    @Mock
    private ServletContextHandler handler;

    @Mock
    private CamelContext camelContext;

    private WebsocketComponent component;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        component = new WebsocketComponent();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createContext()}.
     */
    @Test
    public void testCreateContext() {
        ServletContextHandler handler = component.createContext();
        assertNotNull(handler);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createServer(org.eclipse.jetty.servlet.ServletContextHandler, java.lang.String, int, java.lang.String)}.
     */
    @Test
    public void testCreateServerWithoutStaticContent() {
        ServletContextHandler handler = component.createContext();
        Server server = component.createServer(handler, "localhost", 1988, null);
        assertEquals(1, server.getConnectors().length);
        assertEquals("localhost", server.getConnectors()[0].getHost());
        assertEquals(1988, server.getConnectors()[0].getPort());
        assertFalse(server.getConnectors()[0].isStarted());
        assertEquals(handler, server.getHandler());
        assertEquals(1, server.getHandlers().length);
        assertEquals(handler, server.getHandlers()[0]);
        assertEquals("/", handler.getContextPath());
        assertNotNull(handler.getSessionHandler());
        assertNull(handler.getResourceBase());
        assertNull(handler.getServletHandler().getHolderEntry("/"));
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createServer(org.eclipse.jetty.servlet.ServletContextHandler, java.lang.String, int, java.lang.String)}.
     */
    @Test
    public void testCreateServerWithStaticContent() {
        ServletContextHandler handler = component.createContext();
        Server server = component.createServer(handler, "localhost", 1988, "public/");
        assertEquals(1, server.getConnectors().length);
        assertEquals("localhost", server.getConnectors()[0].getHost());
        assertEquals(1988, server.getConnectors()[0].getPort());
        assertFalse(server.getConnectors()[0].isStarted());
        assertEquals(handler, server.getHandler());
        assertEquals(1, server.getHandlers().length);
        assertEquals(handler, server.getHandlers()[0]);
        assertEquals("/", handler.getContextPath());
        assertNotNull(handler.getSessionHandler());
        assertNotNull(handler.getResourceBase());
        assertTrue(handler.getResourceBase().endsWith("public"));
        assertNotNull(handler.getServletHandler().getHolderEntry("/"));
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createEndpoint(String, String, java.util.Map)}.
     */
    @Test
    public void testCreateEndpoint() throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        Endpoint e1 = component.createEndpoint("websocket://foo", "foo", parameters);
        Endpoint e2 = component.createEndpoint("websocket://foo", "foo", parameters);
        Endpoint e3 = component.createEndpoint("websocket://bar", "bar", parameters);
        assertNotNull(e1);
        assertNotNull(e1);
        assertNotNull(e1);
        assertEquals(e1, e2);
        assertNotSame(e1, e3);
        assertNotSame(e2, e3);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#setServletConsumer(WebsocketComponentServlet, WebsocketConsumer)}.
     */
    @Test
    public void testSetServletConsumer() throws Exception {
        when(servlet.getConsumer()).thenReturn(null, null, consumer);
        InOrder inOrder = inOrder(servlet, consumer, store);
        component.setServletConsumer(servlet, null);        // null && null
        inOrder.verify(servlet, times(0)).setConsumer(null);
        component.setServletConsumer(servlet, consumer);    // null && not null
        inOrder.verify(servlet, times(1)).setConsumer(consumer);
        component.setServletConsumer(servlet, null);        // not null && null
        inOrder.verify(servlet, times(0)).setConsumer(consumer);
        component.setServletConsumer(servlet, consumer);    // not null && not null
        inOrder.verify(servlet, times(0)).setConsumer(consumer);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createServlet(WebsocketStore, String, Map, ServletContextHandler)}.
     */
    @Test
    public void testCreateServlet() throws Exception {
        component.createServlet(store, PATH_SPEC_ONE, servlets, handler);
        InOrder inOrder = inOrder(servlet, consumer, store, servlets, handler);
        ArgumentCaptor<WebsocketComponentServlet> servletCaptor = ArgumentCaptor.forClass(WebsocketComponentServlet.class);
        inOrder.verify(servlets, times(1)).put(eq(PATH_SPEC_ONE), servletCaptor.capture());
        ArgumentCaptor<ServletHolder> holderCaptor = ArgumentCaptor.forClass(ServletHolder.class);
        inOrder.verify(handler, times(1)).addServlet(holderCaptor.capture(), eq(PATH_SPEC_ONE));
        inOrder.verifyNoMoreInteractions();
        assertEquals(servletCaptor.getValue(), holderCaptor.getValue().getServlet());
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#createPathSpec(String)}.
     */
    @Test
    public void testCreatePathSpec() {
        assertEquals(PATH_SPEC_ONE, component.createPathSpec(PATH_ONE));
        assertEquals(PATH_SPEC_TWO, component.createPathSpec(PATH_TWO));
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#addServlet(WebsocketStore, WebsocketConsumer, String)}.
     */
    @Test
    public void testAddServletProducersOnly() throws Exception {
        component.setCamelContext(camelContext);
        component.setPort(0);
        component.doStart();
        WebsocketComponentServlet s1 = component.addServlet(store, null, PATH_ONE);
        WebsocketComponentServlet s2 = component.addServlet(store, null, PATH_TWO);
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
        assertNull(s1.getConsumer());
        assertNull(s2.getConsumer());
        component.doStop();
    }
    
    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#addServlet(WebsocketStore, WebsocketConsumer, String)}.
     */
    @Test
    public void testAddServletConsumersOnly() throws Exception {
        component.setCamelContext(camelContext);
        component.setPort(0);
        component.doStart();
        WebsocketComponentServlet s1 = component.addServlet(store, consumer, PATH_ONE);
        WebsocketComponentServlet s2 = component.addServlet(store, consumer, PATH_TWO);
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
        assertEquals(consumer, s1.getConsumer());
        assertEquals(consumer, s2.getConsumer());
        component.doStop();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#addServlet(WebsocketStore, WebsocketConsumer, String)}.
     */
    @Test
    public void testAddServletProducerAndConsumer() throws Exception {
        component.setCamelContext(camelContext);
        component.setPort(0);
        component.doStart();
        WebsocketComponentServlet s1 = component.addServlet(store, null, PATH_ONE);
        WebsocketComponentServlet s2 = component.addServlet(store, consumer, PATH_ONE);
        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals(s1, s2);
        assertEquals(consumer, s1.getConsumer());
        component.doStop();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketComponent#addServlet(WebsocketStore, WebsocketConsumer, String)}.
     */
    @Test
    public void testAddServletConsumerAndProducer() throws Exception {
        component.setCamelContext(camelContext);
        component.setPort(0);
        component.doStart();
        WebsocketComponentServlet s1 = component.addServlet(store, consumer, PATH_ONE);
        WebsocketComponentServlet s2 = component.addServlet(store, null, PATH_ONE);
        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals(s1, s2);
        assertEquals(consumer, s1.getConsumer());
        component.doStop();
    }
}
