package de.fhg.aisec.dfpolicy;

import java.util.List;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;


public class CamelTest extends CamelBlueprintTestSupport {
    protected static final String TEST_MESSAGE = "Hello World!";
    
    // override this method, and return the location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "test_route.xml";
    }
    
    @Test
    public void testBlockedMessage() throws Exception {
        
    	// Create a mock endpoint for the route
    	MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        // Check that our PEP interceptor has been loaed
        List<InterceptStrategy> interceptors = context.getInterceptStrategies();
        interceptors.forEach(x -> System.out.println(x));
        assertTrue(interceptors
        		.stream()
        		.anyMatch(i -> i.getClass().equals(PEP.class)));
        
        // Send a message into the test route 
        template.sendBody("direct:foo", TEST_MESSAGE);
        
        // We expect the message to NOT arrive at the endpoint, because it is not matched by the rules
        mock.assertIsSatisfied();
    }

}