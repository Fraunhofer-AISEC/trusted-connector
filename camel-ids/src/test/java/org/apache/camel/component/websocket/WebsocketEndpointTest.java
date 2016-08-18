/**
 * 
 */
package org.apache.camel.component.websocket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
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
public class WebsocketEndpointTest {

    private static final String REMAINING = "foo/bar"; 
    private static final String URI       = "websocket://" + REMAINING;
    
    @Mock
    private WebsocketComponent component;

    @Mock
    private Processor processor;

    private WebsocketEndpoint websocketEndpoint;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        websocketEndpoint = new WebsocketEndpoint(URI, component, REMAINING);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketEndpoint#createConsumer(org.apache.camel.Processor)}.
     */
    @Test
    public void testCreateConsumer() throws Exception {
        Consumer consumer = websocketEndpoint.createConsumer(processor);
        assertNotNull(consumer);
        assertEquals(WebsocketConsumer.class, consumer.getClass());
        InOrder inOrder = inOrder(component, processor);
        ArgumentCaptor<WebsocketStore> storeCaptor = ArgumentCaptor.forClass(WebsocketStore.class);
        ArgumentCaptor<WebsocketConsumer> consumerCaptor = ArgumentCaptor.forClass(WebsocketConsumer.class);
        inOrder.verify(component, times(1)).addServlet(storeCaptor.capture(), consumerCaptor.capture(), eq(REMAINING));
        inOrder.verifyNoMoreInteractions();
        assertEquals(MemoryWebsocketStore.class, storeCaptor.getValue().getClass());
        assertEquals(consumer, consumerCaptor.getValue());
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketEndpoint#createProducer()}.
     */
    @Test
    public void testCreateProducer() throws Exception {
        Producer producer = websocketEndpoint.createProducer();
        assertNotNull(producer);
        assertEquals(WebsocketProducer.class, producer.getClass());
        InOrder inOrder = inOrder(component, processor);
        ArgumentCaptor<WebsocketStore> storeCaptor = ArgumentCaptor.forClass(WebsocketStore.class);
        inOrder.verify(component, times(1)).addServlet(storeCaptor.capture(), (WebsocketConsumer)isNull(), eq(REMAINING));
        inOrder.verifyNoMoreInteractions();
        assertEquals(MemoryWebsocketStore.class, storeCaptor.getValue().getClass());
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.WebsocketEndpoint#isSingleton()}.
     */
    @Test
    public void testIsSingleton() {
        assertTrue(websocketEndpoint.isSingleton());
    }
}
