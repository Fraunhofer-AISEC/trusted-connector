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
package de.fhg.aisec.ids.rm

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Test
import java.util.*

class RouteMetricsTest : CamelTestSupport() {
    @Test
    @Throws(Exception::class)
    fun testRouteStats() {
        val MSG_COUNT = 123
        val rm = RouteManagerService()
        val mock = getMockEndpoint("mock:result")
        val bodies = arrayOfNulls<String>(MSG_COUNT)
        Arrays.fill(bodies, TEST_MESSAGE)
        mock.expectedBodiesReceived(listOf(*bodies))

        // Send MSG_COUNT Exchange objects into this route
        for (i in 0 until MSG_COUNT) {
            template.sendBody("direct:input", TEST_MESSAGE)
        }

        // Make sure everything was received at the end of the route
        mock.assertIsSatisfied()

        // Retrieve route statistics from RouteManager and make sure they match expectations
        val after = rm.getRouteStats(
                template.camelContext,
                template.camelContext.adapt(ModelCamelContext::class.java).getRouteDefinition("foo"))
        assertEquals(MSG_COUNT.toLong(), after!!.exchangesCompleted as Long)
        assertEquals(0L, after.exchangesFailed as Long)
        assertEquals(0L, after.redeliveries as Long)
    }

    override fun createRouteBuilders(): Array<RouteBuilder> {
        // Define the most simple route for testing
        val rb: RouteBuilder = object : RouteBuilder() {
            override fun configure() {
                from("direct:input")
                        .routeId("foo")
                        .log(">>> Message from direct to mock: \${body}")
                        .to("mock:result")
            }
        }
        return arrayOf(rb)
    }

    companion object {
        const val TEST_MESSAGE = "Hello World!"
    }
}