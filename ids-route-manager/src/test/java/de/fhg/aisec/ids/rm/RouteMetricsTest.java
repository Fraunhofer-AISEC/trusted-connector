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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.support.dump.RouteStatDump;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Arrays;

public class RouteMetricsTest extends CamelTestSupport {
  protected static final String TEST_MESSAGE = "Hello World!";

  @Test
  public void testRouteStats() throws Exception {
    final int MSG_COUNT = 123;
    RouteManagerService rm = new RouteManagerService();
    MockEndpoint mock = getMockEndpoint("mock:result");
    var bodies = new String[MSG_COUNT];
    Arrays.fill(bodies, TEST_MESSAGE);
    mock.expectedBodiesReceived(Arrays.asList(bodies));

    // Send MSG_COUNT Exchange objects into this route
    for (int i = 0; i < MSG_COUNT; i++) {
      template.sendBody("direct:input", TEST_MESSAGE);
    }

    // Make sure everything was received at the end of the route
    mock.assertIsSatisfied();

    // Retrieve route statistics from RouteManager and make sure they match expectations
    RouteStatDump after =
        rm.getRouteStats(
            template.getCamelContext(),
            template.getCamelContext().adapt(ModelCamelContext.class).getRouteDefinition("foo"));
    assertEquals(MSG_COUNT, (long) after.getExchangesCompleted());
    assertEquals(0L, (long) after.getExchangesFailed());
    assertEquals(0L, (long) after.getRedeliveries());
  }

  @Override
  protected RouteBuilder[] createRouteBuilders() {
    // Define the most simple route for testing
    RouteBuilder rb = new RouteBuilder() {
      public void configure() {
        from("direct:input")
            .routeId("foo")
            .log(">>> Message from direct to mock: ${body}")
            .to("mock:result");
      }
    };
    return new RouteBuilder[] { rb };
  }
}
