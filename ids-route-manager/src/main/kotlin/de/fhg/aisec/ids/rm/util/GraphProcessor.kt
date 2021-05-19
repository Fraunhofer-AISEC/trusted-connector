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
package de.fhg.aisec.ids.rm.util

import de.fhg.aisec.ids.api.router.graph.Edge
import de.fhg.aisec.ids.api.router.graph.GraphData
import de.fhg.aisec.ids.api.router.graph.Node
import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.OptionalIdentifiedDefinition
import org.apache.camel.model.ProcessorDefinition
import org.apache.camel.model.RouteDefinition
import java.util.concurrent.atomic.AtomicInteger

object GraphProcessor {
    /**
     * Prints a single Camel route in Prolog representation.
     *
     * @param route The route to be transformed
     */
    fun processRoute(route: RouteDefinition): GraphData {
        val gd = GraphData()
        // Print route entry points
        processInput(gd, route)
        return gd
    }

    /**
     * Prints a single node of a Camel route in Prolog representation.
     *
     * @param graphData
     * @param current
     * @param preds
     * @return
     */
    private fun processNode(
        graphData: GraphData,
        current: ProcessorDefinition<*>,
        preds: List<OptionalIdentifiedDefinition<*>>
    ): List<ProcessorDefinition<*>> {
        for (p in preds) {
            graphData.addEdge(Edge(p.id, current.id))
        }
        graphData.addNode(
            Node(
                current.id,
                current.label,
                if (current is ChoiceDefinition) Node.NodeType.ChoiceNode else Node.NodeType.Node
            )
        )

        // predecessor of next recursion is the current node
        val newPreds: MutableList<ProcessorDefinition<*>> = ArrayList()
        newPreds.add(current)
        for (out in current.outputs) {
            // if this is a ChoiceDefinition, there is no link between its WhereDefinitions.
            val myPreds = ArrayList<OptionalIdentifiedDefinition<*>>()
            if (current is ChoiceDefinition) {
                myPreds.add(current)
            } else {
                // @TODO: Looks somewhat strange... is this correct?
                myPreds.addAll(newPreds)
            }

            // Recursion ...
            val p = processNode(graphData, out, myPreds)

            // Predecessors of a ChoiceDefinition are all last stmts of its Where- and
            // OtherwiseDefinitions
            if (current is ChoiceDefinition) {
                newPreds.addAll(p)
            } else {
                newPreds.clear()
                newPreds.addAll(p)
            }
        }
        return newPreds
    }

    /** Prints a single FromDefinition (= a route entry point) in Prolog representation. */
    private fun processInput(graphData: GraphData, route: RouteDefinition) {
        val counter = AtomicInteger(0)
        val i = route.input
        // Make sure every input node has a unique id
        if (i.id == null) {
            i.customId = true
            i.id = "input$counter"
        }
        graphData.addNode(Node(i.id, i.label, Node.NodeType.EntryNode))
        var prev: OptionalIdentifiedDefinition<*>? = i
        for (next in route.outputs) {
            processNode(graphData, next, prev?.let { listOf(it) } ?: emptyList())
            prev = next
        }
    }
}
