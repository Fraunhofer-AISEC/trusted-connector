package de.fhg.aisec.ids.rm;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.junit.Test;

import de.fhg.aisec.ids.rm.util.PrologPrinter;

/**
 * Tests conversion of Camel routes into Prolog representation.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class CamelRouteToPrologTest {

	private static final String PROLOG_ROUTE = "node(input0).\n" + 
			"has_action(input0, \"ids-server://0.0.0.0\").\n" + 
			"node(node1).\n" + 
			"has_action(node1, \"convertBodyTo[byte[]]\").\n" + 
			"succ(input0, node1).\n" + 
			"node(node2).\n" + 
			"has_action(node2, \"setProperty[endpointUri]\").\n" + 
			"succ(node1, node2).\n" + 
			"node(node3).\n" + 
			"has_action(node3, \"when[{header{mycond} == A}]choice[when[{header{mycond} == B}]choice[]\").\n" + 
			"succ(node2, node3).\n" + 
			"node(node4).\n" + 
			"has_action(node4, \"when[{header{mycond} == A}]\").\n" + 
			"succ(node3, node4).\n" + 
			"node(node5).\n" + 
			"has_action(node5, \"direct:A1\").\n" + 
			"succ(node4, node5).\n" + 
			"node(node6).\n" + 
			"has_action(node6, \"direct:A2\").\n" + 
			"succ(node5, node6).\n" + 
			"node(node7).\n" + 
			"has_action(node7, \"direct:A3\").\n" + 
			"succ(node6, node7).\n" + 
			"node(node8).\n" + 
			"has_action(node8, \"when[{header{mycond} == B}]\").\n" + 
			"succ(node3, node8).\n" + 
			"node(node9).\n" + 
			"has_action(node9, \"direct:B1\").\n" + 
			"succ(node8, node9).\n" + 
			"node(node10).\n" + 
			"has_action(node10, \"direct:B2\").\n" + 
			"succ(node9, node10).\n" + 
			"node(node11).\n" + 
			"has_action(node11, \"direct:B3\").\n" + 
			"succ(node10, node11).\n" + 
			"node(node12).\n" + 
			"has_action(node12, \"direct:C1otherwise[direct:C2otherwise[direct:C3otherwise[]\").\n" + 
			"succ(node3, node12).\n" + 
			"node(node13).\n" + 
			"has_action(node13, \"direct:C1\").\n" + 
			"succ(node12, node13).\n" + 
			"node(node14).\n" + 
			"has_action(node14, \"direct:C2\").\n" + 
			"succ(node13, node14).\n" + 
			"node(node15).\n" + 
			"has_action(node15, \"direct:C3\").\n" + 
			"succ(node14, node15).\n" + 
			"node(node16).\n" + 
			"has_action(node16, \"log\").\n" + 
			"succ(node3, node16).\n";

	@Test
	public void testCamelRouteToProlog() throws Exception {
		RouteBuilder rb = new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("ids-server://0.0.0.0")
				.routeId("testId")
				.convertBodyTo(byte[].class)
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
		
		StringWriter writer = new StringWriter();
		//prol.printSingleRoute(writer, mockRoute);
		new PrologPrinter().printSingleRoute(writer, mockRoute);
		writer.flush();
		String prolog = writer.getBuffer().toString();
		assertEquals(PROLOG_ROUTE, prolog);
	}
}
