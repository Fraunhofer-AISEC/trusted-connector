/**
 * 
 */
package org.apache.camel.component.websocket;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ProducerOnlyTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(WebsocketComponentLiveTest.class);
    
    @Test
    public void liveTest() throws Exception {
        LOG.info("*** open URL  http://localhost:1989/producer-only.html ***");
        Thread.sleep(1 * 60 * 1000);
    }

    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        
        
        return new RouteBuilder() {
            
            private Counter counter = new Counter();

            public void configure() {
                
                WebsocketComponent component = getContext().getComponent("websocket", WebsocketComponent.class);
                component.setHost("localhost");
                component.setPort(1989);
                component.setStaticResources("src/test/resources");

                from("timer://foo?fixedRate=true&period=1000")
                    .bean(counter)
                    .setHeader(WebsocketConstants.SEND_TO_ALL, constant(true))
                    .to("websocket://counter");
            }
        };
    }

    public class Counter {
        
        private int counter = 0;
        
        public int next() {
            return ++ counter;
        }
    }
}
