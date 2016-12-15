package de.fhg.aisec.ids.webconsole.api.helper;

import static org.apache.camel.util.ObjectHelper.isNotEmpty;

import java.io.PrintWriter;
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
 * Converts Camel route definition into a graphviz visualization.
 * 
 * Shameless plug from http://gitbucket.ms123.org/simpl4/simpl4-src/blob/cea07cf9be5abe751692965c291a2ac4b838dc89/bundles/camel/src/main/java/org/ms123/common/camel/view/RouteDotGenerator.java
 *
 */
public class CamelRouteToDot {
	protected final Map<Object, NodeData> nodeMap = new HashMap<>();
	private int clusterCounter = 0;
	private static final String PREFIX = "http://www.eaipatterns.com/img/";

	protected void printRoutes(PrintWriter writer, Map<String, List<RouteDefinition>> map) {
		Set<Map.Entry<String, List<RouteDefinition>>> entries = map.entrySet();
		for (Map.Entry<String, List<RouteDefinition>> entry : entries) {
			String group = entry.getKey();
			printRoutes(writer, group, entry.getValue());
		}
	}

	protected void printRoutes(PrintWriter writer, String group, List<RouteDefinition> routes) {
		if (group != null) {
			writer.println("subgraph cluster_" + (clusterCounter++) + " {");
			writer.println("label = \"" + group + "\";");
			writer.println("color = grey;");
			writer.println("style = \"dashed\";");
			writer.println("URL = \"" + group + ".html\";");
			writer.println();
		}
		for (RouteDefinition route : routes) {
			List<FromDefinition> inputs = route.getInputs();
			for (FromDefinition input : inputs) {
				printRoute(writer, route, input);
			}
			writer.println();
		}
		if (group != null) {
			writer.println("}");
			writer.println();
		}
	}

	/**
	 * Prints graphviz code of a single RouteDefinition to the provided PrintWriter.
	 * 
	 * @param writer
	 * @param route
	 */
	public void printSingleRoute(PrintWriter writer, final RouteDefinition route) {
		writer.println("digraph {");
		writer.println();

		writer.println("node [style = \"rounded,filled\", fillcolor = white, " + "fontname=\"Helvetica-Oblique\"];");
		List<FromDefinition> inputs = route.getInputs();
		for (FromDefinition input : inputs) {
			printRoute(writer, route, input);
		}
		
		writer.println();
		writer.println("}");
	}
	
	protected void printRoute(PrintWriter writer, final RouteDefinition route, FromDefinition input) {
		NodeData nodeData = getNodeData(input);

		printNode(writer, nodeData);

		NodeData from = nodeData;
		for (ProcessorDefinition<?> output : route.getOutputs()) {
			NodeData newData = printNode(writer, from, output);
			from = newData;
		}
	}

	protected NodeData printNode(PrintWriter writer, NodeData fromData, ProcessorDefinition<?> node) {
		if (node instanceof MulticastDefinition) {
			// no need for a multicast or interceptor node
			List<ProcessorDefinition<?>> outputs = node.getOutputs();
			boolean isPipeline = isPipeline(node);
			for (ProcessorDefinition<?> output : outputs) {
				NodeData out = printNode(writer, fromData, output);
				// if in pipeline then we should move the from node to the next
				// in the pipeline
				if (isPipeline) {
					fromData = out;
				}
			}
			return fromData;
		}
		NodeData toData = getNodeData(node);

		printNode(writer, toData);

		if (fromData != null) {
			writer.print(fromData.id);
			writer.print(" -> ");
			writer.print(toData.id);
			writer.println(" [");

			String label = fromData.edgeLabel;
			if (isNotEmpty(label)) {
				writer.println("label = \"" + label + "\"");
			}
			writer.println("];");
		}

		// now lets write any children
		List<ProcessorDefinition<?>> outputs = toData.outputs;
		if (outputs != null) {
			for (ProcessorDefinition<?> output : outputs) {
				NodeData newData = printNode(writer, toData, output);
				if (!isMulticastNode(node)) {
					toData = newData;
				}
			}
		}
		return toData;
	}

	protected void printNode(PrintWriter writer, NodeData data) {
		if (!data.nodeWritten) {
			data.nodeWritten = true;

			writer.println();
			writer.print(data.id);
			writer.println(" [");
			writer.println("label = \"" + data.label + "\"");
			writer.println("tooltip = \"" + data.tooltop + "\"");
			if (data.url != null) {
				writer.println("URL = \"" + data.url + "\"");
			}

			String image = data.image;
			if (image != null) {
				writer.println("shapefile = \"" + image + "\"");
				writer.println("peripheries=0");
			}
			String shape = data.shape;
			if (shape == null && image != null) {
				shape = "custom";
			}
			if (shape != null) {
				writer.println("shape = \"" + shape + "\"");
			}
			writer.println("];");
			writer.println();
		}
	}

	public void generateFile(PrintWriter writer, Map<String, List<RouteDefinition>> map) {
		writer.println("digraph CamelRoutes {");
		writer.println();

		writer.println("node [style = \"rounded,filled\", fillcolor = white, " + "fontname=\"Helvetica-Oblique\"];");
		writer.println();
		printRoutes(writer, map);

		writer.println("}");
	}

	protected NodeData getNodeData(Object node) {
		Object key = node;
		if (node instanceof FromDefinition) {
			FromDefinition fromType = (FromDefinition) node;
			key = fromType.getUriOrRef();
		} else if (node instanceof ToDefinition) {
			ToDefinition toType = (ToDefinition) node;
			key = toType.getUriOrRef();
		}
		NodeData answer = null;
		if (key != null) {
			answer = nodeMap.get(key);
		}
		if (answer == null) {
			String id = "node" + (nodeMap.size() + 1);
			answer = new NodeData(id, node, PREFIX );
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