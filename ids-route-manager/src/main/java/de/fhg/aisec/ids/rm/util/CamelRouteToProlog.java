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
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.PipelineDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;

/**
 * Converts Camel route definition into a Prolog representation
 *
 */
public class CamelRouteToProlog {
	protected final Map<Object, PrologNode> nodeMap = new HashMap<>();

	protected void printRoutes(Writer writer, Map<String, List<RouteDefinition>> map) throws IOException {
		Set<Map.Entry<String, List<RouteDefinition>>> entries = map.entrySet();
		for (Map.Entry<String, List<RouteDefinition>> entry : entries) {
			String group = entry.getKey();
			printRoutes(writer, group, entry.getValue());
		}
	}

	protected void printRoutes(Writer writer, String group, List<RouteDefinition> routes) throws IOException {
		for (RouteDefinition route : routes) {
			List<FromDefinition> inputs = route.getInputs();
			for (FromDefinition input : inputs) {
				printRoute(writer, route, input);
			}
			writer.write("\n");
		}
	}

	/**
	 * Prints graphviz code of a single RouteDefinition to the provided PrintWriter.
	 *
	 * @param writer
	 * @param route
	 * @throws IOException
	 */
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
		if (node instanceof MulticastDefinition) {
			// no need for a multicast or interceptor node
			List<ProcessorDefinition<?>> outputs = node.getOutputs();
			for (ProcessorDefinition<?> output : outputs) {
				PrologNode out = printNode(writer, fromData, output);
					fromData = out;
			}
			return fromData;
		}
		PrologNode toData = getNodeData(node);
		printNode(writer, toData);

		if (fromData != null) {
			writer.write("succ(" + fromData.id + "," + toData.id +").");
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
			writer.write("node(" + data.id+ ").\n");
			writer.write(nodeToProlog(data));
		}
	}

	private String nodeToProlog(PrologNode data) {
		return "has_url(" + data.id + ", \"" + data.value+ "\").\n";
	}

	public void generateFile(PrintWriter writer, Map<String, List<RouteDefinition>> map) throws IOException {
		writer.println();
		printRoutes(writer, map);
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

	/**
	 * Is the given node a pipeline
	 */
	protected boolean isPipeline(ProcessorDefinition<?> node) {
		if (node instanceof MulticastDefinition) {
			return false;
		}
		if (node instanceof PipelineDefinition) {
			return true;
		}
		if (node.getOutputs().size() > 1) {
			// is pipeline if there is more than 1 output and they are all To
			// types
			for (Object type : node.getOutputs()) {
				if (!(type instanceof ToDefinition)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
