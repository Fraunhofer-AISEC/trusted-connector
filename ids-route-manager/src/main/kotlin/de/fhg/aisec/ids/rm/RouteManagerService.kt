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
import de.fhg.aisec.ids.api.router.RouteMetrics
import de.fhg.aisec.ids.api.router.RouteObject
import de.fhg.aisec.ids.rm.util.CamelRouteToDot
import de.fhg.aisec.ids.rm.util.PrologPrinter
import org.apache.camel.CamelContext
import org.apache.camel.Endpoint
import org.apache.camel.ServiceStatus
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.management.DefaultManagementAgent
import org.apache.camel.model.ModelCamelContext
import org.apache.camel.model.ModelHelper
import org.apache.camel.model.RouteDefinition
import org.apache.camel.spring.boot.ComponentConfigurationPropertiesCommon
import org.apache.camel.support.dump.RouteStatDump
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import javax.management.AttributeNotFoundException
import javax.management.InstanceNotFoundException
import javax.management.MBeanException
import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import javax.management.ReflectionException
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException

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

    override val routeMetrics: Map<String, RouteMetrics>
        get() {
            val rdump: MutableMap<String, RouteMetrics> = HashMap()
            val cCtxs = camelContexts
            for (cCtx in cCtxs) {
                val mcc = cCtx.adapt(ModelCamelContext::class.java)
                val rds = mcc.routeDefinitions
                for (rd in rds) {
                    var stat: RouteStatDump?
                    try {
                        stat = getRouteStats(cCtx, rd)
                        if (stat != null) {
                            val m = RouteMetrics()
                            m.completed = stat.exchangesCompleted
                            m.redeliveries = stat.redeliveries
                            m.failed = stat.exchangesFailed
                            m.failuresHandled = stat.failuresHandled
                            m.inflight = stat.exchangesInflight
                            m.maxProcessingTime = stat.maxProcessingTime
                            m.minProcessingTime = stat.minProcessingTime
                            m.meanProcessingTime = stat.meanProcessingTime
                            rdump[rd.id] = m
                        }
                    } catch (e: MalformedObjectNameException) {
                        LOG.error(e.message, e)
                    } catch (e: AttributeNotFoundException) {
                        LOG.error(e.message, e)
                    } catch (e: InstanceNotFoundException) {
                        LOG.error(e.message, e)
                    } catch (e: MBeanException) {
                        LOG.error(e.message, e)
                    } catch (e: ReflectionException) {
                        LOG.error(e.message, e)
                    } catch (e: JAXBException) {
                        LOG.error(e.message, e)
                    }
                }
            }
            return rdump
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

    @Throws(
        MalformedObjectNameException::class,
        JAXBException::class,
        AttributeNotFoundException::class,
        InstanceNotFoundException::class,
        MBeanException::class,
        ReflectionException::class
    )
    fun getRouteStats(cCtx: CamelContext, rd: RouteDefinition): RouteStatDump? {
        val context = JAXBContext.newInstance(RouteStatDump::class.java)
        val unmarshaller = context.createUnmarshaller()
        val agent = cCtx.managementStrategy.managementAgent
        if (agent != null) {
            val mBeanServer = agent.mBeanServer
            val set =
                mBeanServer.queryNames(
                    ObjectName(
                        DefaultManagementAgent.DEFAULT_DOMAIN +
                            ":type=routes,name=\"" +
                            rd.id +
                            "\",*"
                    ),
                    null
                )
            for (routeMBean in set) {
                // the route must be part of the camel context
                val camelId = mBeanServer.getAttribute(routeMBean, "CamelId") as String?
                if (camelId != null && camelId == cCtx.name) {
                    val xml =
                        mBeanServer.invoke(
                            routeMBean,
                            "dumpRouteStatsAsXml",
                            arrayOf<Any>(false, true),
                            arrayOf("boolean", "boolean")
                        ) as
                            String
                    return unmarshaller.unmarshal(StringReader(xml)) as RouteStatDump
                }
            }
        }
        return null
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

    /**
     * Retrieves the textual representation of a route
     *
     * @param routeId The id of the route that is to be exported
     */
    override fun getRouteAsString(routeId: String): String? {
        for (c in camelContexts) {
            val rd = c.adapt(ModelCamelContext::class.java).getRouteDefinition(routeId) ?: continue
            try {
                return ModelHelper.dumpModelAsXml(c, rd)
            } catch (e: JAXBException) {
                LOG.error(e.message, e)
            }
        }
        return null
    }

    /**
     * Save a route, replacing it with a new representation within the same context
     *
     * @param routeId ID of the route to save
     * @param routeRepresentation The new textual representation of the route (XML etc.)
     * @throws RouteException If the route does not exist or some Exception was thrown during route
     * replacement.
     */
    @Throws(RouteException::class)
    override fun saveRoute(routeId: String, routeRepresentation: String): RouteObject {
        LOG.debug("Save route \"$routeId\": $routeRepresentation")
        var cCtx: CamelContext? = null
        var routeStarted = false

        // Find the state and CamelContext of the route to be saved
        for (c in camelContexts) {
            val targetRoute = c.getRoute(routeId)
            if (targetRoute != null) {
                cCtx = c
                val serviceStatus = cCtx.routeController.getRouteStatus(routeId)
                routeStarted =
                    serviceStatus == ServiceStatus.Started ||
                    serviceStatus == ServiceStatus.Starting
                break
            }
        }
        if (cCtx == null) {
            LOG.error("Could not find route with id \"$routeId\"")
            throw RouteException("Could not find route with id \"$routeId\"")
        }

        // Check for validity of route representation
        var routes: List<RouteDefinition>
        try {
            ByteArrayInputStream(routeRepresentation.toByteArray(StandardCharsets.UTF_8)).use { bis
                ->
                // Load route(s) from XML
                val rd = ModelHelper.loadRoutesDefinition(cCtx, bis)
                routes = rd.routes
                val id =
                    routes
                        .stream()
                        .map { obj: RouteDefinition -> obj.id }
                        .filter { rid: String -> routeId != rid }
                        .findAny()
                if (id.isPresent) {
                    throw Exception(
                        "The new route representation has a different ID: " +
                            "Expected \"" +
                            routeId +
                            "\" but got \"" +
                            id.get() +
                            "\""
                    )
                }
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            throw RouteException(e)
        }

        // Remove old route from CamelContext
        try {
            cCtx.removeRoute(routeId)
        } catch (e: Exception) {
            LOG.error("Error while removing old route \"$routeId\"", e)
            throw RouteException(e)
        }

        // Add new route and start it if it was started/starting before save
        return try {
            val routeDefinition = routes[0]
            cCtx.adapt(ModelCamelContext::class.java).addRouteDefinition(routeDefinition)
            if (routeStarted) {
                cCtx.routeController.startRoute(routeDefinition.id)
            }
            routeDefinitionToObject(cCtx, routeDefinition)
        } catch (e: Exception) {
            LOG.error("Error while adding new route \"$routeId\"", e)
            throw RouteException(e)
        }
    }

    /**
     * Create a new route in a fresh context from text
     *
     * @param routeDefinition The textual representation of the route to be inserted
     * @throws RouteException If a route with that name already exists
     */
    @Throws(RouteException::class)
    override fun addRoute(routeDefinition: String) {
        LOG.debug("Adding new route: $routeDefinition")
        val existingRoutes = this.routes
        val cCtx: CamelContext = DefaultCamelContext()
        try {
            ByteArrayInputStream(routeDefinition.toByteArray(StandardCharsets.UTF_8)).use { bis
                ->
                // Load route(s) from XML
                val rd = ModelHelper.loadRoutesDefinition(cCtx, bis)
                val routes = rd.routes
                // Check that intersection of existing and new routes is empty (=we do not allow
                // overwriting
                // existing route ids)
                val intersect =
                    routes
                        .stream()
                        .filter { r -> existingRoutes.stream().anyMatch { it.id == r.id } }
                        .map { it.id }
                        .collect(Collectors.toList())
                if (intersect.isNotEmpty()) {
                    throw RouteException(
                        "Route id already exists. Will not overwrite it. ${intersect.joinToString(", ")}"
                    )
                }
                cCtx.adapt(ModelCamelContext::class.java).addRouteDefinitions(routes)
                cCtx.start()
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            throw RouteException(e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RouteManagerService::class.java)
    }
}
