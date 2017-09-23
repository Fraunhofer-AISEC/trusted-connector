package de.fhg.aisec.ids.rm;

import java.io.PrintWriter;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.junit.Test;

import de.fhg.aisec.ids.rm.util.CamelRouteToProlog;
import de.fhg.aisec.ids.rm.util.PrologPrinter;

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
		           .when(header("mycond").isEqualTo("A"))
		               .to("direct:A1")
		               .to("direct:A2")
		               .to("direct:A3")
		           .when(header("mycond").isEqualTo("B"))
		               .to("direct:B1")
		               .to("direct:B2")
		               .to("direct:B3")
		           .otherwise()
		               .to("direct:C1")
		               .to("direct:C2")
		               .to("direct:C3")
	               .end()
				.log("Endpoint ${property.endpointUri} received body: ${body}, headers: ${headers}");
				
			}
			
		};
		
		rb.configure();
		RouteDefinition mockRoute = rb.getRouteCollection().getRoutes().get(0);
		
		PrintWriter writer = new PrintWriter(System.out);
		//prol.printSingleRoute(writer, mockRoute);
		new PrologPrinter().printSingleRoute(writer, mockRoute);
		writer.flush();
	}
}
