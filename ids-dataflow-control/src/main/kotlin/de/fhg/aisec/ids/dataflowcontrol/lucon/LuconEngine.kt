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
package de.fhg.aisec.ids.dataflowcontrol.lucon

import alice.tuprolog.Prolog
import alice.tuprolog.SolveInfo
import alice.tuprolog.Theory
import alice.tuprolog.event.LibraryEvent
import alice.tuprolog.exceptions.InvalidLibraryException
import alice.tuprolog.exceptions.InvalidTheoryException
import alice.tuprolog.exceptions.MalformedGoalException
import alice.tuprolog.exceptions.NoSolutionException
import alice.tuprolog.interfaces.event.LibraryListener
import de.fhg.aisec.ids.api.router.CounterExample
import de.fhg.aisec.ids.api.router.RouteVerificationProof
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * LUCON (Logic based Usage Control) policy decision engine.
 *
 * This engine uses tuProlog as a logic language implementation to answer policy decision requests.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
class LuconEngine
/**
 * Create a new LuconEngine which writes to a given output stream.
 *
 * @param out OutputStream to write Prolog engine outputs to, or null if output should not printed.
 */
(out: OutputStream?) {
    private val p: Prolog = Prolog()

    val theory: String
        get() {
            val t = p.theory
            return t?.toString() ?: ""
        }

    init {
        try {
            p.theory = Theory.parseWithStandardOperators(defaultPolicy)
        } catch (e: Exception) {
            LOG.error("Error loading default policy", e)
        }

        // Add some listeners for logging/debugging
        p.addExceptionListener { ex -> LOG.error("Exception in Prolog reasoning: " + ex.msg) }
        if (LOG.isTraceEnabled) {
            p.addQueryListener { q -> LOG.trace("Prolog query " + q.solveInfo.query.toString()) }
        }
        if (LOG.isDebugEnabled) {
            p.addLibraryListener(
                object : LibraryListener {
                    override fun libraryLoaded(e: LibraryEvent) {
                        LOG.debug("Prolog library loaded " + e.libraryName)
                    }

                    override fun libraryUnloaded(e: LibraryEvent) {
                        LOG.debug("Prolog library unloaded " + e.libraryName)
                    }
                }
            )
        }
        if (LOG.isTraceEnabled) {
            p.addSpyListener { l -> LOG.trace(l.msg + " " + l.source) }
        }
        p.addWarningListener { warningEvent ->
            val w = warningEvent.msg
            if (WARNING_FILTER.matcher(w).matches()) {
                if (LOG.isTraceEnabled) {
                    LOG.trace(w)
                }
            } else {
                if (LOG.isWarnEnabled) {
                    LOG.warn(w)
                }
            }
        }
        p.addOutputListener { l ->
            if (out != null) {
                try {
                    out.write(l.msg.toByteArray(StandardCharsets.UTF_8))
                } catch (e: Exception) {
                    LOG.error(e.message, e)
                }
            }
        }

        try {
            p.loadLibrary(LuconLibrary())
        } catch (e: InvalidLibraryException) {
            // should never happen
            throw RuntimeException("Error loading " + LuconLibrary::class.java.name, e)
        }
    }

    fun setSpy(spy: Boolean) {
        p.isSpy = spy
    }

    /**
     * Loads a policy in form of a prolog theory.
     *
     * Existing policies will be overwritten.
     *
     * @param theory The theory to load
     */
    @Throws(InvalidTheoryException::class)
    fun loadPolicy(theory: String) {
        val t = Theory.parseWithStandardOperators(theory)
        LOG.debug("Loading theory:\n$t")
        p.theory = t
    }

    @Throws(MalformedGoalException::class)
    fun query(
        query: String?,
        findAll: Boolean
    ): List<SolveInfo> {
        if (LOG.isTraceEnabled) {
            LOG.trace("Running Prolog query: " + query!!)
        }
        val result = query(p, query, findAll)
        if (LOG.isTraceEnabled) {
            try {
                for (i in result) {
                    LOG.trace("Result is {}", i.solution.toString())
                }
            } catch (e: NoSolutionException) {
                e.printStackTrace()
            }
        }
        return result
    }

    private fun query(
        engine: Prolog,
        query: String?,
        findAll: Boolean
    ): List<SolveInfo> {
        val result = ArrayList<SolveInfo>()
        if (query == null) {
            return result
        }
        var solution = engine.solve(query)
        while (solution.isSuccess) {
            result.add(solution)
            if (findAll && engine.hasOpenAlternatives()) {
                solution = engine.solveNext()
            } else {
                break
            }
        }
        engine.solveEnd()
        return result
    }

    /**
     * Returns "true" if the given route is valid under all policies or returns a set of
     * counterexamples.
     *
     * @param id Route id
     * @param routePl The route, represented as Prolog
     * @return A list of counterexamples which violate the rule or empty, if no route violates the
     * policy.
     */
    fun proofInvalidRoute(
        id: String,
        routePl: String
    ): RouteVerificationProof {
        // The proof object we will return
        val proof = RouteVerificationProof(id)

        // Just for information: save the query we used to generate the proof
        proof.query = QUERY_ROUTE_VERIFICATION

        try {
            // Get policy as prolog, add Camel route and init new Prolog engine with combined theory
            val t = p.theory
            t.append(Theory.parseWithStandardOperators(routePl))
            val newP = Prolog()
            newP.loadLibrary(LuconLibrary())
            newP.theory = t

            // Generate the proof (=run query)
            val result = query(newP, QUERY_ROUTE_VERIFICATION, true)

            // If a result has been found, this means there is at least one counterexample of a path
            // in a
            // route that violates a policy
            if (result.isNotEmpty()) {
                val ces = ArrayList<CounterExample>(result.size)
                result.forEach { s ->
                    try {
                        ces.add(CounterExampleImpl(s.getVarValue("T")))
                    } catch (nse: NoSolutionException) {
                        // This cannot happen if our code wasn't badly screwed up!
                        throw RuntimeException(nse)
                    }
                }
                proof.counterExamples = ces
                proof.isValid = false
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
        }

        return proof
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LuconEngine::class.java)
        private var defaultPolicy = ""

        // A Prolog query to compute a path from X to Y in a graph of statements (= a route)
        private const val QUERY_ROUTE_VERIFICATION = "entrynode(X), stmt(Y), path(X, Y, T)."
        private val WARNING_FILTER = Pattern.compile("^WARNING: The predicate .* is unknown\\.$")

        fun setDefaultPolicy(theory: String) {
            defaultPolicy = theory
        }
    }
}
