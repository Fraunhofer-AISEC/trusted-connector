package de.fhg.aisec.ids.rm;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.RouteStatDump;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.fhg.aisec.ids.api.router.RouteMetrics;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RouteMetricsTest extends CamelTestSupport {
    protected static final String TEST_MESSAGE = "Hello World!";
    protected List<Object> messages;

    /**
     * Switching on JMX support to get access to context.getManagementStrategy().getManagementAgent().
     */
    @Override
    protected boolean useJmx() {
        return true;
    }
    
	@Test
    public void testRouteStats() throws Exception {
		final int MSG_COUNT = 123;
		RouteManagerService rm = new RouteManagerService();
		MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        // Send MSG_COUNT Exchange objects into this route
        for (int i=0;i<MSG_COUNT;i++) {
            template.sendBody("direct:input", TEST_MESSAGE);
        }

        // Make sure everything was received at the end of the route
        MockEndpoint.expectsMessageCount(MSG_COUNT, mock);
        mock.assertIsSatisfied();
        
        // Retrieve route statistics from RouteManager and make sure they match expectations
        RouteMetrics after = rm.getRouteStats(template.getCamelContext(), template.getCamelContext().getRouteDefinition("foo"));
        assertEquals(MSG_COUNT, (long) after.getCompleted());
        assertEquals(0l, (long) after.getFailed());
        assertEquals(0l, (long) after.getRedeliveries());
        assertEquals(0l, (long) after.getFailuresHandled());
        assertEquals(0l, (long) after.getInflight());
    }
    
    @Override
    protected RouteBuilder[] createRouteBuilders() throws Exception {
        RouteBuilder[] rbs = new RouteBuilder[1];        

        // Define the most simple route for testing
        rbs[0] = new RouteBuilder() {
            public void configure() throws MalformedURLException {
		        from("direct:input").routeId("foo")
                	.log(">>> Message from direct to mock : ${body}")
                	.to("mock:result");
                }
        };
        return rbs;
    }
}