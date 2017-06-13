package de.fhg.aisec.ids.rm.util;

import static org.apache.camel.util.ObjectHelper.isNotEmpty;

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
 * Converts Camel route definition into a graphviz visualization.
 *
 * Shameless plug from http://gitbucket.ms123.org/simpl4/simpl4-src/blob/cea07cf9be5abe751692965c291a2ac4b838dc89/bundles/camel/src/main/java/org/ms123/common/camel/view/RouteDotGenerator.java
 *
 */
public class CamelRouteToDot {
	protected final Map<Object, NodeData> nodeMap = new HashMap<>();
	private int clusterCounter = 0;
	private static final String PREFIX = "http://www.eaipatterns.com/img/";

	protected void printRoutes(Writer writer, Map<String, List<RouteDefinition>> map) throws IOException {
		Set<Map.Entry<String, List<RouteDefinition>>> entries = map.entrySet();
		for (Map.Entry<String, List<RouteDefinition>> entry : entries) {
			String group = entry.getKey();
			printRoutes(writer, group, entry.getValue());
		}
	}

	protected void printRoutes(Writer writer, String group, List<RouteDefinition> routes) throws IOException {
		if (group != null) {
			writer.write("subgraph cluster_" + (clusterCounter++) + " {\n");
			writer.write("label = \"" + group + "\";\n");
			writer.write("color = grey;\n");
			writer.write("style = \"dashed\";\n");
			//writer.write("URL = \"" + group + ".html\";\n\n");
		}
		for (RouteDefinition route : routes) {
			List<FromDefinition> inputs = route.getInputs();
			for (FromDefinition input : inputs) {
				printRoute(writer, route, input);
			}
			writer.write("\n");
		}
		if (group != null) {
			writer.write("}\n\n");
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
		writer.write("digraph { rankdir=LR; size=\"4.5,5.5\" \n\n");
		writer.write("node [shape=\"box\", style = \"filled\", fillcolor = white, " + "fontname=\"Helvetica-Oblique\"];");
		List<FromDefinition> inputs = route.getInputs();
		for (FromDefinition input : inputs) {
			printRoute(writer, route, input);
		}

		writer.write("\n}");
	}

	protected void printRoute(Writer writer, final RouteDefinition route, FromDefinition input) throws IOException {
		NodeData nodeData = getNodeData(input);

		printNode(writer, nodeData);

		NodeData from = nodeData;
		for (ProcessorDefinition<?> output : route.getOutputs()) {
			NodeData newData = printNode(writer, from, output);
			from = newData;
		}
	}

	protected NodeData printNode(Writer writer, NodeData fromData, ProcessorDefinition<?> node) throws IOException {
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
			writer.write(fromData.id);
			writer.write(" -> ");
			writer.write(toData.id);
			writer.write(" [\n");

			String label = fromData.edgeLabel;
			if (isNotEmpty(label)) {
				writer.write("label = \"" +  label.substring(0,Math.min(8, label.length())) + (label.length()>8?"..":"") + "\"\n");
			}
			writer.write("];\n");
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

	protected void printNode(Writer writer, NodeData data) throws IOException {
		if (!data.nodeWritten) {
			data.nodeWritten = true;

			writer.write("\n");
			writer.write(data.id + "\n");
			writer.write(" [\n");
			writer.write("label = \"" + data.label.substring(0,Math.min(12, data.label.length())) + (data.label.length()>12?"..":"") + "\"\n");
			writer.write("tooltip = \"" + data.tooltip + "\"\n");
			/*if (data.url != null) {
				writer.write("URL = \"" + data.url + "\"\n");
			}*/

			String image = data.image;
			if (image != null) {
				writer.write("shapefile = \"" + image + "\"\n");
				writer.write("peripheries=0");
			}
			String shape = data.shape;
			if (shape == null && image != null) {
				shape = "custom";
			}
			if (shape != null) {
				writer.write("shape = \"" + shape + "\"\n");
			}
			writer.write("];\n\n");
		}
	}

	public void generateFile(PrintWriter writer, Map<String, List<RouteDefinition>> map) throws IOException {
		writer.println("digraph CamelRoutes {");
		writer.println();

		writer.println("node [style = \"rounded,filled\", fillcolor = white, color = \"#898989\", " + "fontname=\"Helvetica-Oblique\"];");
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
