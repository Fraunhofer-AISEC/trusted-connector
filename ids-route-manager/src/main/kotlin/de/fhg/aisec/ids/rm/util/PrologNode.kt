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

/** Represents a node in the EIP diagram tree */
class PrologNode(node: Any) {
    // 	public String id;
    private var nodeType: String? = null
    private var value: String? = null

    // 	public String predicate = "has_url";
    private var outputs: List<ProcessorDefinition<*>>? = null

    init {
        // 		this.id = id;
        if (node is ProcessorDefinition<*>) {
            // 			this.predicate = "has_operation";
            value = node.label
        }
        when (node) {
            is FromDefinition -> {
                nodeType = "from"
                // 			this.predicate = "has_url";
                value = node.endpointUri
            }
            is ToDefinition -> {
                value = node.endpointUri
                nodeType = "to"
            }
            is FilterDefinition -> {
                nodeType = "message_filter"
            }
            is WhenDefinition -> {
                nodeType = "when"
                value = node.expression.expression
            }
            is OtherwiseDefinition -> {
                nodeType = "otherwise"
                value = ""
            }
            is ChoiceDefinition -> {
                val outputs: MutableList<ProcessorDefinition<*>> = ArrayList(node.whenClauses)
                if (node.otherwise != null) {
                    outputs.add(node.otherwise)
                }
                this.outputs = outputs
                nodeType = "choice"
            }
            is RecipientListDefinition<*> -> {
                // 			this.predicate = "recipient_list";
                value = node.label
                nodeType = "recipients"
            }
            is RoutingSlipDefinition<*> -> {
                value = node.label
                nodeType = "slip"
            }
            is SplitDefinition -> {
                nodeType = "splitter"
            }
            is AggregateDefinition -> {
                nodeType = "aggregator"
            }
            is ResequenceDefinition -> {
                // 			this.predicate = "resequence";
                value = node.label
            }
            is BeanDefinition -> {
                // 			this.predicate = "bean";
                value = node.label
            }
            is TransformDefinition -> {
                value = node.label
                // 			this.predicate = "transform";
            }
        }

        // lets auto-default as many values as we can
        if (ObjectHelper.isEmpty(nodeType)) {
            var name = node.javaClass.name
            val idx = name.lastIndexOf('.')
            if (idx > 0) {
                name = name.substring(idx + 1)
            }
            if (name.endsWith("Type")) {
                name = name.substring(0, name.length - 4)
            }
            nodeType = name
        }
        if (ObjectHelper.isEmpty(value) && ObjectHelper.isNotEmpty(nodeType)) {
            value = nodeType
        }
        if (node is ProcessorDefinition<*> && outputs == null) {
            outputs = node.outputs
        }
    }
}
