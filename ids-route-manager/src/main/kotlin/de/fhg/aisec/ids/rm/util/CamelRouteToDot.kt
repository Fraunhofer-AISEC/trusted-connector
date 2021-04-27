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

import java.io.IOException
import java.io.PrintWriter
import java.io.Writer
import java.util.*
import org.apache.camel.model.*
import org.apache.camel.util.ObjectHelper

/**
 * Camel route definition to GraphViz converter.
 *
 * The output can be turned into pictures using dot, neato, and others.
 */
class CamelRouteToDot {
    private val nodeMap: MutableMap<Any?, NodeData> = IdentityHashMap()
    private var clusterCounter = 0

    @Throws(IOException::class)
    private fun printRoutes(writer: Writer, map: Map<String, List<RouteDefinition>>) {
        val entries = map.entries
        for ((group, value) in entries) {
            printRoutes(writer, group, value)
        }
    }

    @Throws(IOException::class)
    private fun printRoutes(writer: Writer, group: String?, routes: List<RouteDefinition>) {
        if (group != null) {
            writer.write("""subgraph cluster_${clusterCounter++} {
""")
            writer.write("label = \"$group\";\n")
            writer.write("color = grey;\n")
            writer.write("style = \"dashed\";\n")
        }
        for (route in routes) {
            printRoute(writer, route, route.input)
            writer.write("\n")
        }
        if (group != null) {
            writer.write("}\n\n")
        }
    }

    /**
     * Prints graphviz code of a single RouteDefinition to the provided PrintWriter.
     *
     * @param writer
     * @param route
     * @throws IOException
     */
    @Throws(IOException::class)
    fun printSingleRoute(writer: Writer?, route: RouteDefinition?) {
        if (writer == null || route == null) {
            return
        }
        writer.write("digraph { rankdir=LR; size=\"4.5,5.5\" \n\n")
        writer.write(
            "node [shape=\"box\", style = \"filled\", fillcolor = white, " +
                "fontname=\"Helvetica-Oblique\"];"
        )
        printRoute(writer, route, route.input)
        writer.write("\n}")
    }

    @Throws(IOException::class)
    private fun printRoute(writer: Writer, route: RouteDefinition, input: FromDefinition) {
        val nodeData = getNodeData(input)
        printNode(writer, nodeData)
        var from: NodeData? = nodeData
        for (output in route.outputs) {
            from = printNode(writer, from, output)
        }
    }

    @Throws(IOException::class)
    private fun printNode(
        writer: Writer,
        fromData: NodeData?,
        node: ProcessorDefinition<*>?
    ): NodeData? {
        var fromDataVar = fromData
        if (node is MulticastDefinition) {
            // no need for a multicast or interceptor node
            val outputs = node.getOutputs()
            val isPipeline = isPipeline(node)
            for (output in outputs) {
                val out = printNode(writer, fromDataVar, output)
                // if in pipeline then we should move the from node to the next
                // in the pipeline
                if (isPipeline) {
                    fromDataVar = out
                }
            }
            return fromDataVar
        }
        var toData: NodeData? = getNodeData(node!!)
        printNode(writer, toData)
        if (fromDataVar != null) {
            writer.write(fromDataVar.id)
            writer.write(" -> ")
            writer.write(toData!!.id)
            writer.write(" [\n")
            val label = fromDataVar.edgeLabel
            if (ObjectHelper.isNotEmpty(label)) {
                writer.write("label = \"$label\"\n")
            }
            writer.write("];\n")
        }

        // now lets write any children
        val outputs = toData!!.outputs
        if (outputs != null) {
            for (output in outputs) {
                val newData = printNode(writer, toData, output)
                if (!isMulticastNode(node)) {
                    toData = newData
                }
            }
        }
        return toData
    }

    @Throws(IOException::class)
    private fun printNode(writer: Writer, data: NodeData?) {
        if (!data!!.nodeWritten) {
            data.nodeWritten = true
            writer.write("\n")
            writer.write("""
    ${data.id}
    
    """.trimIndent())
            writer.write(" [\n")
            writer.write("""
    label = "${data.label}"
    
    """.trimIndent())
            writer.write("""
    tooltip = "${data.tooltip}"
    
    """.trimIndent())
            val image = data.image
            if (image != null) {
                writer.write("shapefile = \"$image\"\n")
                writer.write("peripheries=0")
            }
            var shape = data.shape
            if (shape == null && image != null) {
                shape = "custom"
            }
            if (shape != null) {
                writer.write("shape = \"$shape\"\n")
            }
            writer.write("];\n\n")
        }
    }

    @Throws(IOException::class)
    fun generateFile(writer: PrintWriter?, map: Map<String, List<RouteDefinition>>?) {
        if (writer == null || map == null) {
            return
        }
        writer.println("digraph CamelRoutes {")
        writer.println()
        writer.println(
            "node [style = \"rounded,filled\", fillcolor = white, color = \"#898989\", " +
                "fontname=\"Helvetica-Oblique\"];"
        )
        writer.println()
        printRoutes(writer, map)
        writer.println("}")
    }

    private fun getNodeData(node: Any): NodeData {
        var key = node
        if (node is FromDefinition) {
            key = node.uri
        } else if (node is ToDefinition) {
            key = node.uri
        }
        var answer: NodeData? = nodeMap[key]
        if (answer == null) {
            val id = "node" + (nodeMap.size + 1)
            answer = NodeData(id, node, PREFIX)
            nodeMap[key] = answer
        }
        return answer
    }

    private fun isMulticastNode(node: ProcessorDefinition<*>?): Boolean {
        return node is MulticastDefinition || node is ChoiceDefinition
    }

    /** Is the given node a pipeline */
    private fun isPipeline(node: ProcessorDefinition<*>): Boolean {
        if (node is MulticastDefinition) {
            return false
        }
        if (node is PipelineDefinition) {
            return true
        }
        if (node.outputs.size > 1) {
            // is pipeline if there is more than 1 output and they are all To
            // types
            for (type in node.outputs) {
                if (type !is ToDefinition) {
                    return false
                }
            }
            return true
        }
        return false
    }

    companion object {
        private const val PREFIX = "http://www.eaipatterns.com/img/"
    }
}
