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

import org.apache.camel.model.AggregateDefinition
import org.apache.camel.model.BeanDefinition
import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.FilterDefinition
import org.apache.camel.model.FromDefinition
import org.apache.camel.model.OtherwiseDefinition
import org.apache.camel.model.ProcessorDefinition
import org.apache.camel.model.RecipientListDefinition
import org.apache.camel.model.ResequenceDefinition
import org.apache.camel.model.RoutingSlipDefinition
import org.apache.camel.model.SplitDefinition
import org.apache.camel.model.ToDefinition
import org.apache.camel.model.TransformDefinition
import org.apache.camel.model.WhenDefinition
import org.apache.camel.util.ObjectHelper
import java.util.Locale

/** Represents a node in Graphviz representation of a route. */
class NodeData(var id: String, node: Any?, imagePrefix: String) {
    var image: String? = null
    var label: String? = null
    var shape: String? = null
    var edgeLabel: String? = null
    var tooltip: String? = null
    var nodeType: String? = null
    var nodeWritten = false
    var url: String? = null
    var outputs: List<ProcessorDefinition<*>>? = null

    private fun removeQueryString(text: String?): String? {
        return text?.indexOf('?')?.let { idx ->
            if (idx <= 0) {
                text
            } else {
                text.substring(0, idx)
            }
        }
    }

    companion object {
        /** Inserts a space before each upper case letter after a lowercase */
        fun insertSpacesBetweenCamelCase(name: String): String {
            var lastCharacterLowerCase = false
            val buffer = StringBuilder()
            var i = 0
            val size = name.length
            while (i < size) {
                val ch = name[i]
                lastCharacterLowerCase =
                    if (Character.isUpperCase(ch)) {
                        if (lastCharacterLowerCase) {
                            buffer.append(' ')
                        }
                        false
                    } else {
                        true
                    }
                buffer.append(ch)
                i++
            }
            return buffer.toString()
        }
    }

    init {
        if (node is ProcessorDefinition<*>) {
            edgeLabel = node.label
        }
        when (node) {
            is FromDefinition -> {
                tooltip = node.label
                label = removeQueryString(tooltip)
                url = "http://camel.apache.org/message-endpoint.html"
            }
            is ToDefinition -> {
                tooltip = node.label
                label = removeQueryString(tooltip)
                edgeLabel = ""
                url = "http://camel.apache.org/message-endpoint.html"
            }
            is FilterDefinition -> {
                image = imagePrefix + "MessageFilterIcon.png"
                label = "Filter"
                nodeType = "Message Filter"
            }
            is WhenDefinition -> {
                image = imagePrefix + "MessageFilterIcon.png"
                nodeType = "When Filter"
                label = "When"
                url = "http://camel.apache.org/content-based-router.html"
            }
            is OtherwiseDefinition -> {
                nodeType = "Otherwise"
                edgeLabel = ""
                url = "http://camel.apache.org/content-based-router.html"
                tooltip = "Otherwise"
            }
            is ChoiceDefinition -> {
                image = imagePrefix + "ContentBasedRouterIcon.png"
                nodeType = "Content Based Router"
                label = "Choice"
                edgeLabel = ""
                val choice = node
                val outputs: MutableList<ProcessorDefinition<*>> = ArrayList(choice.whenClauses)
                if (choice.otherwise != null) {
                    outputs.add(choice.otherwise)
                }
                this.outputs = outputs
            }
            is RecipientListDefinition<*> -> {
                image = imagePrefix + "RecipientListIcon.png"
                nodeType = "Recipient List"
            }
            is RoutingSlipDefinition<*> -> {
                image = imagePrefix + "RoutingTableIcon.png"
                nodeType = "Routing Slip"
                url = "http://camel.apache.org/routing-slip.html"
            }
            is SplitDefinition -> {
                image = imagePrefix + "SplitterIcon.png"
                nodeType = "Splitter"
            }
            is AggregateDefinition -> {
                image = imagePrefix + "AggregatorIcon.png"
                nodeType = "Aggregator"
            }
            is ResequenceDefinition -> {
                image = imagePrefix + "ResequencerIcon.png"
                nodeType = "Resequencer"
            }
            is BeanDefinition -> {
                nodeType = "Bean Ref"
                label = node.label + " Bean"
                shape = "box"
            }
            is TransformDefinition -> {
                nodeType = "Transform"
                url = "http://camel.apache.org/message-translator.html"
            }
        }

        // lets auto-default as many values as we can
        if (ObjectHelper.isEmpty(nodeType) && node != null) {
            var name = node.javaClass.name
            val idx = name.lastIndexOf('.')
            if (idx > 0) {
                name = name.substring(idx + 1)
            }
            if (name.endsWith("Type")) {
                name = name.substring(0, name.length - 4)
            }
            nodeType = insertSpacesBetweenCamelCase(name)
        }
        if (label == null) {
            if (ObjectHelper.isEmpty(image)) {
                label = nodeType
                shape = "box"
            } else if (ObjectHelper.isNotEmpty(edgeLabel)) {
                label = ""
            } else {
                label = node.toString()
            }
        }
        if (ObjectHelper.isEmpty(tooltip)) {
            if (ObjectHelper.isNotEmpty(nodeType)) {
                val description = if (ObjectHelper.isNotEmpty(edgeLabel)) edgeLabel else label
                tooltip = nodeType + ": " + description
            } else {
                tooltip = label
            }
        }
        if (ObjectHelper.isEmpty(url) && ObjectHelper.isNotEmpty(nodeType)) {
            url =
                (
                    "http://camel.apache.org/" +
                        nodeType!!.toLowerCase(Locale.ENGLISH).replace(' ', '-') +
                        ".html"
                    )
        }
        if (node is ProcessorDefinition<*> && outputs == null) {
            outputs = node.outputs
        }
    }
}
