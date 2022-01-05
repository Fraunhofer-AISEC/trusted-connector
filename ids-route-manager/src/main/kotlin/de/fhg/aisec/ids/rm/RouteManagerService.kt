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

import de.fhg.aisec.ids.api.router.RouteComponent
import de.fhg.aisec.ids.api.router.RouteException
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.api.router.RouteObject
import de.fhg.aisec.ids.rm.util.CamelRouteToDot
import de.fhg.aisec.ids.rm.util.PrologPrinter
import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.RouteDefinition
import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

/**
 * Manages Camel routes.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Component
class RouteManagerService : RouteManager {
    @Autowired private lateinit var ctx: ApplicationContext

    override val routes: List<RouteObject>
        get() {
            val result: MutableList<RouteObject> = ArrayList()
            val camelContexts = camelContexts

            // Create response
            for (cCtx in camelContexts) {
                val mcc = cCtx.adapt(ModelCamelContext::class.java)
                for (rd in mcc.routeDefinitions) {
                    result.add(routeDefinitionToObject(cCtx, rd))
                }
            }
            return result
        }

    override fun getRoute(id: String): RouteObject? {
        val camelContexts = camelContexts

        // Create response
        for (cCtx in camelContexts) {
            val mcc = cCtx.adapt(ModelCamelContext::class.java)
            val rd = mcc.getRouteDefinition(id)
            if (rd != null) {
                return routeDefinitionToObject(cCtx, rd)
            }
        }
        return null
    }

    @Throws(RouteException::class)
    override fun startRoute(routeId: String) {
        val camelC = camelContexts
        for (cCtx in camelC) {
            val rt = cCtx.getRoute(routeId)
            if (rt != null) {
                try {
                    cCtx.routeController.startRoute(routeId)
                } catch (e: Exception) {
                    throw RouteException(e)
                }
            }
        }
    }

    @Throws(RouteException::class)
    override fun stopRoute(routeId: String) {
        val camelC = camelContexts
        for (cCtx in camelC) {
            val rt = cCtx.getRoute(routeId)
            if (rt != null) {
                try {
                    cCtx.routeController.stopRoute(routeId)
                } catch (e: Exception) {
                    throw RouteException(e)
                }
            }
        }
    }

    override fun listComponents(): List<RouteComponent> {
        val beanNamesForType =
            ctx.getBeanNamesForType(ComponentConfigurationPropertiesCommon::class.java)

        // yes, this is a bit hacky but will do fow now
        return beanNamesForType
            .mapNotNull { name ->
                val first = name.split("-org")[0]

                if (first == "camel.component") {
                    null
                } else {
                    RouteComponent("camel-" + first.split("camel.component.")[1], "")
                }
            }
    }

    override val endpoints: Map<String, Collection<String>>
        get() {
            return camelContexts
                .stream()
                .collect(
                    Collectors.toMap(
                        { obj: CamelContext -> obj.name },
                        { c: CamelContext -> c.endpoints.map { obj: Endpoint -> obj.endpointUri } }
                    )
                )
        }

    override fun listEndpoints(): Map<String, String> {
        val epURIs: MutableMap<String, String> = HashMap()
        for (cCtx in camelContexts) {
            for ((key, value) in cCtx.endpointRegistry) {
                epURIs[key!!.get()] = value.endpointUri
            }
        }
        return epURIs
    }

    override fun delRoute(routeId: String) {
        val cCtxs = camelContexts
        for (cCtx in cCtxs) {
            val mcc = cCtx.adapt(ModelCamelContext::class.java)
            for (rd in mcc.routeDefinitions) {
                if (rd.id == routeId) {
                    try {
                        cCtx.removeRoute(rd.id)
                    } catch (e: Exception) {
                        LOG.error(e.message, e)
                    }
                    return
                }
            }
        }
    }

    // sort the list
    private val camelContexts: List<CamelContext>
        get() {
            return try {
                val contexts = ctx.getBeansOfType(CamelContext::class.java).values.toMutableList()

                contexts.sortWith(Comparator.comparing { it.name })

                return contexts
            } catch (e: Exception) {
                LOG.warn("Cannot retrieve the list of Camel contexts.", e)
                emptyList()
            }
        }

    /**
     * Wraps a RouteDefinition in a RouteObject for use over API.
     *
     * @param cCtx Camel Context
     * @param rd The RouteDefinition to be transformed
     * @return The resulting RouteObject
     */
    private fun routeDefinitionToObject(cCtx: CamelContext, rd: RouteDefinition): RouteObject {
        return RouteObject(
            rd.id,
            rd.descriptionText,
            routeToDot(rd),
            rd.shortName,
            cCtx.name,
            cCtx.uptimeMillis,
            cCtx.routeController.getRouteStatus(rd.id).toString()
        )
    }

    /**
     * Creates a visualization of a Camel route in DOT (graphviz) format.
     *
     * @param rd The route definition to process
     * @return The string representation of the Camel route in DOT
     */
    private fun routeToDot(rd: RouteDefinition): String {
        var result = ""
        try {
            val viz = CamelRouteToDot()
            val bos = ByteArrayOutputStream()
            val writer = BufferedWriter(OutputStreamWriter(bos, StandardCharsets.UTF_8))
            viz.printSingleRoute(writer, rd)
            writer.flush()
            result = bos.toString(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            LOG.error(e.message, e)
        }
        return result
    }

    override fun getRouteInputUris(routeId: String): List<String> {
        for (ctx in camelContexts) {
            val mcc = ctx.adapt(ModelCamelContext::class.java)
            for (rd in mcc.routeDefinitions) {
                if (routeId == rd.id) {
                    return listOf(rd.input.uri)
                }
            }
        }
        return emptyList()
    }

    /**
     * Retrieves the Prolog representation of a route
     *
     * @param routeId The id of the route that is to be exported
     */
    override fun getRouteAsProlog(routeId: String): String {
        val c =
            camelContexts
                .parallelStream()
                .filter { cCtx: CamelContext ->
                    cCtx.adapt(ModelCamelContext::class.java).getRouteDefinition(routeId) != null
                }
                .findAny()
        if (c.isPresent) {
            try {
                val rd = c.get().adapt(ModelCamelContext::class.java).getRouteDefinition(routeId)
                val writer = StringWriter()
                PrologPrinter().printSingleRoute(writer, rd)
                writer.flush()
                return writer.toString()
            } catch (e: IOException) {
                LOG.error("Error printing route to prolog $routeId", e)
            }
        }
        return ""
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RouteManagerService::class.java)
    }
}
