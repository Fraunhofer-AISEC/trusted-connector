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

import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.OptionalIdentifiedDefinition
import org.apache.camel.model.ProcessorDefinition
import org.apache.camel.model.RouteDefinition
import java.io.IOException
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger

class PrologPrinter {
    /**
     * Prints a single Camel route in Prolog representation.
     *
     * @param writer
     * @param route
     * @throws IOException
     */
    @Throws(IOException::class)
    fun printSingleRoute(
        writer: Writer?,
        route: RouteDefinition?
    ) {
        if (writer == null || route == null) {
            return
        }

        // Print route entry points
        printInput(writer, route)
    }

    /**
     * Prints a single node of a Camel route in Prolog representation.
     *
     * @param writer
     * @param current
     * @param preds
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun printNode(
        writer: Writer,
        current: ProcessorDefinition<*>,
        preds: List<OptionalIdentifiedDefinition<*>>
    ): List<ProcessorDefinition<*>> {
        for (p in preds) {
            writer.write(
                """
                succ(${p.id}, ${current.id}).
                
                """.trimIndent()
            )
        }
        writer.write(
            """
            stmt(${current.id}).
            
            """.trimIndent()
        )
        writer.write(
            """
            has_action(${current.id}, "${current.label}").
            
            """.trimIndent()
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
            val p = printNode(writer, out, myPreds)

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

    /**
     * Prints a single FromDefinition (= a route entry point) in Prolog representation.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun printInput(
        writer: Writer,
        route: RouteDefinition
    ) {
        val counter = AtomicInteger(0)
        val i = route.input
        // Make sure every input node has a unique id
        if (i.id == null) {
            i.customId = true
            i.id = "input" + counter.incrementAndGet()
        }
        writer.write(
            """
            stmt(${i.id}).
            
            """.trimIndent()
        )
        writer.write(
            """
            entrynode(${i.id}).
            
            """.trimIndent()
        )
        writer.write(
            """
            has_action(${i.id}, "${i.label}").
            
            """.trimIndent()
        )
        var prev: OptionalIdentifiedDefinition<*>? = i
        for (next in route.outputs) {
            printNode(writer, next, prev?.let { listOf(it) } ?: emptyList())
            prev = next
        }
    }
}
