package de.fhg.aisec.ids.rm.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.WhenDefinition;

public class PrologPrinter {
	
	public void printSingleRoute(PrintWriter writer, RouteDefinition mockRoute) {
		print(writer, mockRoute.getInputs(), mockRoute);
	}

	private void print(PrintWriter writer, List<FromDefinition> inputs, RouteDefinition mockRoute) {
		for (FromDefinition i : inputs) {
			print(writer, i);
		}
		
		
		List<ProcessorDefinition<?>> preds = new ArrayList<>();
		print(0, writer, mockRoute, preds);
	}

	private List<ProcessorDefinition<?>> print(int indent, PrintWriter writer, ProcessorDefinition<?> current, List<ProcessorDefinition<?>> preds) {
		for (int i=0;i<indent;i++) {writer.write(" ");	}
		
		writer.write("node(node"+current.getIndex() + ").\n");
		writer.write("has_action(node"+current.getIndex() + ", \"" + current.getLabel() + "\").\n");
		for (ProcessorDefinition<?> p : preds) {
			//for (int i=0;i<indent;i++) {writer.write(" ");	}
			writer.write("succ(node" + p.getIndex() +", node" + current.getIndex() + ").\n");
		}

		// predecessor of next recursion is the current node 
		List<ProcessorDefinition<?>> newPreds = new ArrayList<>();
		newPreds.add(current);
		for (ProcessorDefinition<?> out : current.getOutputs()) {
			
			// if this is a ChoiceDefinition, there is no link between its WhereDefinitions.
			ArrayList<ProcessorDefinition<?>> myPreds = new ArrayList<>();
			if (current instanceof ChoiceDefinition) {
				myPreds.add(current);
			} else {
				myPreds.addAll(newPreds);
			}
			
			// Recursion ...
			List<ProcessorDefinition<?>> p = print(indent+1, writer, out, myPreds);
			
			// Predecessors of a ChoiceDefinition are all last stmts of its Where- and OtherwiseDefinitions
			if (current instanceof ChoiceDefinition) {
				newPreds.addAll(p);
			} else {
				newPreds.clear(); newPreds.addAll(p);
			}
		}
		
		if (current instanceof WhenDefinition) {
			System.out.println("when returns " + String.join(", ", newPreds.stream().map(p -> String.valueOf(p.getIndex())).collect(Collectors.toList())));
		}
		
		if (current instanceof ChoiceDefinition) {
			newPreds.remove(current);
			System.out.println("choice returns " + String.join(", ", newPreds.stream().map(p -> String.valueOf(p.getIndex())).collect(Collectors.toList())));
		}
		
		return newPreds;
	}

	private void print(PrintWriter writer, FromDefinition i) {
		writer.write("Input " + i.getLabel() + "\n");
	}

}
