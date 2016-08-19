/**
 * 
 */
package de.fhg.camel.ids;

import static org.mockito.Mockito.*;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.spi.ExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.fhg.camel.ids.WebsocketConstants;
import de.fhg.camel.ids.WebsocketConsumer;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WebsocketConsumerTest {
    
    private static final String CONNECTION_KEY = "random-connection-key";
    private static final String MESSAGE = "message";
    
    @Mock
    private Endpoint endpoint;

    @Mock
    private ExceptionHandler exceptionHandler;

    @Mock
    private Processor processor;

    @Mock
    private Exchange exchange;
    
    @Mock
    private Message outMessage;
    
    private Exception exception = new Exception("BAD NEWS EVERYONE!");

    private WebsocketConsumer websocketConsumer;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        websocketConsumer = new WebsocketConsumer(endpoint, processor);
        websocketConsumer.setExceptionHandler(exceptionHandler);
    }

    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketConsumer#sendExchange(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testSendExchange() throws Exception {
        when(endpoint.createExchange()).thenReturn(exchange);
        when(exchange.getOut()).thenReturn(outMessage);
        
        websocketConsumer.sendExchange(CONNECTION_KEY, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, exceptionHandler, processor, exchange, outMessage);
        inOrder.verify(endpoint, times(1)).createExchange();
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setHeader(WebsocketConstants.CONNECTION_KEY, CONNECTION_KEY);
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setBody(MESSAGE);
        inOrder.verify(processor, times(1)).process(exchange);
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketConsumer#sendExchange(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testSendExchangeWithException() throws Exception {
        when(endpoint.createExchange()).thenReturn(exchange);
        when(exchange.getOut()).thenReturn(outMessage);
        doThrow(exception).when(processor).process(exchange);
        when(exchange.getException()).thenReturn(exception);
        
        websocketConsumer.sendExchange(CONNECTION_KEY, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, exceptionHandler, processor, exchange, outMessage);
        inOrder.verify(endpoint, times(1)).createExchange();
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setHeader(WebsocketConstants.CONNECTION_KEY, CONNECTION_KEY);
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setBody(MESSAGE);
        inOrder.verify(processor, times(1)).process(exchange);
        inOrder.verify(exchange, times(2)).getException();
        inOrder.verify(exceptionHandler, times(1)).handleException(any(String.class), eq(exchange), eq(exception));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link de.fhg.camel.ids.WebsocketConsumer#sendExchange(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testSendExchangeWithExchangeExceptionIsNull() throws Exception {
        when(endpoint.createExchange()).thenReturn(exchange);
        when(exchange.getOut()).thenReturn(outMessage);
        doThrow(exception).when(processor).process(exchange);
        when(exchange.getException()).thenReturn(null);
        
        websocketConsumer.sendExchange(CONNECTION_KEY, MESSAGE);
        
        InOrder inOrder = inOrder(endpoint, exceptionHandler, processor, exchange, outMessage);
        inOrder.verify(endpoint, times(1)).createExchange();
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setHeader(WebsocketConstants.CONNECTION_KEY, CONNECTION_KEY);
        inOrder.verify(exchange, times(1)).getOut();
        inOrder.verify(outMessage, times(1)).setBody(MESSAGE);
        inOrder.verify(processor, times(1)).process(exchange);
        inOrder.verify(exchange, times(1)).getException();
        inOrder.verifyNoMoreInteractions();
    }
}
