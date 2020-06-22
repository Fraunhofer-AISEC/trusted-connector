/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.dataflowcontrol

import alice.tuprolog.SolveInfo
import alice.tuprolog.Var
import alice.tuprolog.exceptions.MalformedGoalException
import alice.tuprolog.exceptions.NoMoreSolutionException
import alice.tuprolog.exceptions.NoSolutionException
import alice.tuprolog.exceptions.PrologException
import com.google.common.cache.CacheBuilder
import de.fhg.aisec.ids.api.policy.*
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.api.router.RouteVerificationProof
import de.fhg.aisec.ids.dataflowcontrol.lucon.LuconEngine
import de.fhg.aisec.ids.dataflowcontrol.lucon.TuPrologHelper.escape
import de.fhg.aisec.ids.dataflowcontrol.lucon.TuPrologHelper.listStream
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import org.osgi.service.component.ComponentContext
import org.osgi.service.component.annotations.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * This is a singleton, i.e. there will only be one instance of PolicyDecisionPoint within the whole
 * runtime.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-dataflow-control")
@org.springframework.stereotype.Component
class PolicyDecisionPoint : PDP, PAP {

    // Convenience val for this thread's LuconEngine instance
    private val engine: LuconEngine
        get() = threadEngine.get()

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Volatile
    @Autowired(required = false)
    private var routeManager: RouteManager? = null

    private val transformationCache =
        CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build<ServiceNode, TransformationDecision>()

    /**
     * Creates a query to retrieve policy decision from Prolog knowledge base.
     *
     * @param target The target node of the transformation
     * @param labels The exchange properties
     */
    private fun createDecisionQuery(target: ServiceNode, labels: Set<String>): String {
        val sb = StringBuilder()
        sb.append("rule(X), has_target(X, T), ")
        sb.append("has_endpoint(T, EP), ")
        sb.append("regex_match(EP, ").append(escape(target.endpoint)).append("), ")
        // Assert labels for the duration of this query, must be done before receives_label(X)
        labels.forEach { k -> sb.append("assert(label(").append(k).append(")), ") }
        sb.append("receives_label(X), ")
        sb.append("rule_priority(X, P), ")
        // Removed due to unclear relevance
        //        if (target.capabilties.size + target.properties.size > 0) {
        //            val capProp = LinkedList<String>()
        //            for (cap in target.capabilties) {
        //                capProp.add("has_capability(T, " + escape(cap) + ")")
        //            }
        //            for (prop in target.properties) {
        //                capProp.add("has_property(T, " + escape(prop) + ")")
        //            }
        //            sb.append("(").append(capProp.joinToString(", ")).append("), ")
        //        }
        sb.append(
            "(has_decision(X, D) ; (has_obligation(X, _O), has_alternativedecision(_O, Alt), "
        )
        sb.append("requires_prerequisite(_O, A)))")
        if (labels.isNotEmpty()) {
            // Cleanup prolog VM for next run
            sb.append(", retractall(label(_))")
        }
        sb.append(".")
        return sb.toString()
    }

    /**
     * A transformation query retrieves the set of labels to add and to remove from the Prolog
     * knowledge base.
     *
     * This method returns the respective query for a specific target.
     *
     * @param target The ServiceNode to be processed
     * @return The resulting Prolog query for the transformation
     */
    private fun createTransformationQuery(target: ServiceNode): String {
        val sb = StringBuilder()
        val plEndpoint: String
        if (target.endpoint != null) {
            plEndpoint = escape(target.endpoint)
            //            sb.append("dominant_allow_rules(").append(plEndpoint).append(", _T, _), ")
        } else {
            throw RuntimeException("No endpoint specified!")
        }
        // Removed due to unclear relevance
        //        if (target.capabilties.size + target.properties.size > 0) {
        //            val capProp = LinkedList<String>()
        //            for (cap in target.capabilties) {
        //                capProp.add("has_capability(_T, " + escape(cap) + ")")
        //            }
        //            for (prop in target.properties) {
        //                capProp.add("has_property(_T, " + escape(prop) + ")")
        //            }
        //            sb.append('(').append(capProp.joinToString(", ")).append("),\n")
        //        }
        sb.append("once(setof(S, action_service(")
            .append(plEndpoint)
            .append(", S), SC); SC = []),\n")
            .append("collect_creates_labels(SC, ACraw), set_of(ACraw, Adds),\n")
            .append("collect_removes_labels(SC, RCraw), set_of(RCraw, Removes).")
        return sb.toString()
    }

    @Activate
    @Suppress("UNUSED_PARAMETER")
    private fun activate(ignored: ComponentContext) {
        loadPolicies()
    }

    @PostConstruct
    fun loadPolicies() {
        // Try to load existing policies from deploy dir at activation
        // val dir = File(System.getProperty("karaf.base") + File.separator + "deploy")
        val url = Thread.currentThread().contextClassLoader.getResource("deploy") ?: return
        val file = File(url.path)

        val directoryListing = file.listFiles()
        if (directoryListing == null || !file.isDirectory) {
            LOG.warn("Unexpected or not running in karaf: Not a directory: " + file.absolutePath)
            return
        }

        var loaded = false
        for (f in directoryListing) {
            if (f.name.endsWith(LUCON_FILE_EXTENSION)) {
                if (!loaded) {
                    LOG.info("Loading Lucon policy from " + f.absolutePath)
                    loadPolicy(f.readText())
                    loaded = true
                } else {
                    LOG.warn("Multiple policy files. Will load only one! " + f.absolutePath)
                }
            }
        }
    }

    override fun requestTranformations(lastServiceNode: ServiceNode): TransformationDecision {
        try {
            return transformationCache.get(lastServiceNode) {
                // Query prolog for labels to remove or add from message
                val query = this.createTransformationQuery(lastServiceNode)
                if (LOG.isDebugEnabled) {
                    LOG.debug("Query for uncached label transformation: $query")
                }

                val result = TransformationDecision()
                try {
                    val solveInfo = this.engine.query(query, true)
                    if (solveInfo.isNotEmpty()) {
                        // Get solutions, convert label variables to string and collect in sets
                        val labelsToAdd = result.labelsToAdd
                        val labelsToRemove = result.labelsToRemove
                        solveInfo.forEach { s ->
                            try {
                                val adds = s.getVarValue("Adds").term
                                if (adds.isList) {
                                    listStream(adds).forEach { labelsToAdd.add(it.toString()) }
                                } else {
                                    throw RuntimeException("\"Adds\" is not a prolog list!")
                                }
                                val removes = s.getVarValue("Removes").term
                                if (removes.isList) {
                                    listStream(removes).forEach {
                                        labelsToRemove.add(it.toString())
                                    }
                                } else {
                                    throw RuntimeException("\"Removes\" is not a prolog list!")
                                }
                            } catch (ignored: NoSolutionException) {}
                        }
                    }
                    LOG.debug("Transformation: {}", result)
                } catch (e: Throwable) {
                    LOG.error(e.message, e)
                }

                result
            }
        } catch (ee: ExecutionException) {
            LOG.error(ee.message, ee)
            return TransformationDecision()
        }
    }

    override fun requestDecision(req: DecisionRequest): PolicyDecision {
        val dec = PolicyDecision()
        if (LOG.isTraceEnabled) {
            LOG.trace("Decision requested " + req.from.endpoint + " -> " + req.to.endpoint)
        }

        try {
            // Query Prolog engine for a policy decision
            val startTime = System.nanoTime()
            val query = this.createDecisionQuery(req.to, req.labels)
            if (LOG.isTraceEnabled) {
                LOG.trace("Decision query: {}", query)
            }
            val solveInfo = this.engine.query(query, true)
            val time = System.nanoTime() - startTime
            if (LOG.isTraceEnabled) {
                LOG.trace("Decision query took {} ms", time / 1e6f)
            }

            // If there is no matching rule, deny by default
            if (solveInfo.isEmpty()) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("No policy decision found. Returning " + dec.decision.toString())
                }
                dec.reason = "No matching rule"
                return dec
            }

            // Include only solveInfos with highest priority
            var maxPrio = Integer.MIN_VALUE
            val applicableSolveInfos = ArrayList<SolveInfo>()
            for (si in solveInfo) {
                try {
                    val priority = Integer.parseInt(si.getVarValue("P").term.toString())
                    if (priority > maxPrio) {
                        maxPrio = priority
                        applicableSolveInfos.clear()
                    }
                    if (priority == maxPrio) {
                        applicableSolveInfos.add(si)
                    }
                } catch (e: NumberFormatException) {
                    LOG.warn("Invalid rule priority: " + si.getVarValue("P"), e)
                } catch (e: NullPointerException) {
                    LOG.warn("Invalid rule priority: " + si.getVarValue("P"), e)
                }
            }

            // Just for debugging
            if (LOG.isDebugEnabled) {
                debug(applicableSolveInfos)
            }

            // Collect obligations
            val obligations = LinkedList<Obligation>()
            applicableSolveInfos.forEach { s ->
                try {
                    val rule = s.getVarValue("X")
                    val decision = s.getVarValue("D")
                    if (decision !is Var) {
                        val decString = decision.term.toString()
                        if ("drop" == decString) {
                            dec.reason = rule.term.toString()
                        } else if ("allow" == decString) {
                            dec.reason = rule.term.toString()
                            dec.decision = Decision.ALLOW
                        }
                    }
                    val action = s.getVarValue("A")
                    val altDecision = s.getVarValue("Alt")
                    if (action !is Var) {
                        val o = Obligation()
                        o.action = action.term.toString()
                        if (altDecision !is Var) {
                            val altDecString = altDecision.term.toString()
                            if ("drop" == altDecString) {
                                o.alternativeDecision = Decision.DENY
                            } else if ("allow" == altDecString) {
                                o.alternativeDecision = Decision.ALLOW
                            }
                        }
                        obligations.add(o)
                    }
                } catch (e: NoSolutionException) {
                    LOG.warn("Unexpected: solution variable not present: " + e.message)
                    dec.reason = "Solution variable not present"
                }
            }
            dec.obligations = obligations
        } catch (e: NoMoreSolutionException) {
            LOG.error(e.message, e)
            dec.reason = "Error: " + e.message
        } catch (e: MalformedGoalException) {
            LOG.error(e.message, e)
            dec.reason = "Error: " + e.message
        } catch (e: NoSolutionException) {
            LOG.error(e.message, e)
            dec.reason = "Error: " + e.message
        }

        return dec
    }

    /**
     * Just for debugging: Print query solution to DEBUG out.
     *
     * @param solveInfo A list of Prolog solutions
     */
    private fun debug(solveInfo: List<SolveInfo>) {
        if (!LOG.isTraceEnabled) {
            return
        }
        try {
            for (i in solveInfo) {
                if (i.isSuccess) {
                    val vars = i.bindingVars
                    LOG.trace(
                        vars.joinToString(", ") { v ->
                            String.format(
                                "%s: %s (%s)",
                                v.name,
                                v.term,
                                if (v.isBound) "bound" else "unbound"
                            )
                        }
                    )
                }
            }
        } catch (nse: NoSolutionException) {
            LOG.trace("No solution found", nse)
        }
    }

    override fun clearAllCaches() {
        // clear Prolog cache entries
        try {
            engine.query("cache_clear(_).", true)
            LOG.info("Prolog cache_clear(_) successful")
        } catch (pe: PrologException) {
            LOG.warn("Prolog cache_clear(_) failed", pe)
        }

        // clear transformation cache
        transformationCache.invalidateAll()
    }

    override fun loadPolicy(theory: String?) {
        // Load policy into engine, possibly overwriting the existing one.
        this.engine.loadPolicy(theory ?: "")
        LuconEngine.setDefaultPolicy(theory ?: "")
    }

    override fun listRules(): List<String> {
        return try {
            val rules = this.engine.query("rule(X).", true)
            rules.map { it.getVarValue("X").toString() }.toList()
        } catch (e: PrologException) {
            LOG.error("Prolog error while retrieving rules " + e.message, e)
            emptyList()
        }
    }

    override fun getPolicy(): String {
        return this.engine.theory
    }

    override fun verifyRoute(routeId: String): RouteVerificationProof? {
        val rm = this.routeManager
        if (rm == null) {
            LOG.warn("No RouteManager. Cannot verify Camel route $routeId")
            return null
        }

        val routePl = rm.getRouteAsProlog(routeId)

        return engine.proofInvalidRoute(routeId, routePl)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PolicyDecisionPoint::class.java)
        private const val LUCON_FILE_EXTENSION = ".pl"

        // Each thread creates a LuconEngine instance to prevent concurrency issues
        val threadEngine: ThreadLocal<LuconEngine> =
            ThreadLocal.withInitial { LuconEngine(System.out) }
    }
}
