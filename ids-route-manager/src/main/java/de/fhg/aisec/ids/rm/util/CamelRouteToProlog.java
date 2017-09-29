/*-
 * ========================LICENSE_START=================================
 * IDS Container Manager
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.aisec.ids.rm.util;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.OtherwiseDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.WhenDefinition;

/**
 * Converts Camel route definition into a Prolog representation
 *
 */
public class CamelRouteToProlog {
	protected final Map<Object, PrologNode> nodeMap = new HashMap<>();
	public void printSingleRoute(Writer writer, final RouteDefinition route) throws IOException {
		List<FromDefinition> inputs = route.getInputs();
		for (FromDefinition input : inputs) {
			printRoute(writer, route, input);
		}

		writer.write("\n}");
	}

	protected void printRoute(Writer writer, final RouteDefinition route, FromDefinition input) throws IOException {
		PrologNode nodeData = getNodeData(input);

		printNode(writer, nodeData);

		PrologNode from = nodeData;
		for (ProcessorDefinition<?> output : route.getOutputs()) {
			PrologNode newData = printNode(writer, from, output);
			from = newData;
		}
	}

	protected PrologNode printNode(Writer writer, PrologNode fromData, ProcessorDefinition<?> node) throws IOException {
		PrologNode toData = getNodeData(node);
		printNode(writer, toData);

		if ( fromData != null) {
			// Successors of choice blocks are (only) the successors of the last statements in each when/otherwise block. 
			if (!"choice".equals(fromData.nodeType) || 
					(node instanceof WhenDefinition || node instanceof OtherwiseDefinition)) {
				writer.write("succ(" + fromData.id + "," + toData.id +").");
			}
		}

		if ("choice".equals(fromData.nodeType) ) {
			System.out.println(fromData.id + " node: " + node.getLabel());
			//Last node in block
			System.out.println(node.getParent().getOutputs().get(node.getParent().getOutputs().size()-1).getLabel());
			for (ProcessorDefinition<?> o : node.getParent().getParent().getOutputs()) {
				System.out.println("   " +o.getLabel());
			}
		}
		
		// now lets write any children
		List<ProcessorDefinition<?>> outputs = toData.outputs;
		if (outputs != null) {
			for (ProcessorDefinition<?> output : outputs) {
				PrologNode newData = printNode(writer, toData, output);
				if (!isMulticastNode(node)) {
					toData = newData;
				}
			}
		}
		return toData;
	}

	protected void printNode(Writer writer, PrologNode data) throws IOException {
		if (!data.nodeWritten) {
			data.nodeWritten = true;
			writer.write("\n");
			writer.write(data.nodeType + "(" + data.id+ ").\n");
			writer.write(nodeToProlog(data));
		}
	}

	private String nodeToProlog(PrologNode data) {
		return "has_url(" + data.id + ", \"" + data.value+ "\").\n";
	}


	protected PrologNode getNodeData(Object node) {
		Object key = node;
		if (node instanceof FromDefinition) {
			FromDefinition fromType = (FromDefinition) node;
			key = fromType.getUriOrRef();
		} else if (node instanceof ToDefinition) {
			ToDefinition toType = (ToDefinition) node;
			key = toType.getUriOrRef();
		}
		PrologNode answer = null;
		if (key != null) {
			answer = nodeMap.get(key);
		}
		if (answer == null) {
			String id = "node" + (nodeMap.size() + 1);
			answer = new PrologNode(id, node );
			nodeMap.put(key, answer);
		}
		return answer;
	}

	protected boolean isMulticastNode(ProcessorDefinition<?> node) {
		return node instanceof MulticastDefinition || node instanceof ChoiceDefinition;
	}
}
