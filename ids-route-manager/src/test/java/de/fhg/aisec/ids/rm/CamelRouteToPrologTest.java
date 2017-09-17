package de.fhg.aisec.ids.rm;

import java.io.PrintWriter;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.junit.Test;

import de.fhg.aisec.ids.rm.util.CamelRouteToProlog;

public class CamelRouteToPrologTest {

	@Test
	public void testCamelRouteToProlog() throws Exception {
		CamelRouteToProlog prol = new CamelRouteToProlog();

		RouteBuilder rb = new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("ids-server://0.0.0.0")
				.convertBodyTo(byte[].class)
				.routeId("testId")
				.setProperty("endpointUri", new ConstantExpression("ids-client://0.0.0.0"))
	           .choice()
	           .when(header("foo").isEqualTo("bar"))
	               .to("direct:b")
	           .when(header("foo").isEqualTo("cheese"))
	               .to("direct:c")
	           .otherwise()
	               .to("direct:d")
	               .end()
				.log("Endpoint ${property.endpointUri} received body: ${body}, headers: ${headers}");
				
			}
			
		};
		
		rb.configure();
		RouteDefinition mockRoute = rb.getRouteCollection().getRoutes().get(0);
		
		PrintWriter writer = new PrintWriter(System.out);
		prol.printSingleRoute(writer, mockRoute);
		writer.flush();
	}
}
