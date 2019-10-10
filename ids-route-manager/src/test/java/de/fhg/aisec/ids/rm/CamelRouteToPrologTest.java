/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.rm;

import static org.junit.Assert.assertTrue;

import de.fhg.aisec.ids.rm.util.PrologPrinter;
import java.io.StringWriter;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.junit.Test;

/**
 * Tests conversion of Camel routes into Prolog representation.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class CamelRouteToPrologTest {

  @Test
  public void testCamelRouteToProlog() throws Exception {
    RouteBuilder rb =
        new RouteBuilder() {
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
                .log(
                    "Endpoint ${property.endpointUri} received body: ${body}, headers: ${headers}");
          }
        };
    rb.configure();
    RouteDefinition mockRoute = rb.getRouteCollection().getRoutes().get(0);

    StringWriter writer = new StringWriter();
    new PrologPrinter().printSingleRoute(writer, mockRoute);
    writer.flush();
    String prolog = writer.getBuffer().toString();
    assertTrue(
        prolog.contains("when[{header{mycond} == A}]choice[when[{header{mycond} == B}]choice[]"));
    assertTrue(prolog.contains("ids-server://0.0.0.0"));
    assertTrue(prolog.contains("direct:A1"));
    System.out.println(prolog);
  }
}
