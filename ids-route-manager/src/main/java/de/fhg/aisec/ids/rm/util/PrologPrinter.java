/*-
 * ========================LICENSE_START=================================
 * IDS Route Manager
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
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;

public class PrologPrinter {
	
	/**
	 * Prints a single Camel route in Prolog representation.
	 * 
	 * @param writer
	 * @param route
	 * @throws IOException 
	 */
	public void printSingleRoute(Writer writer, RouteDefinition route) throws IOException {
		// Print route entry points
		printInputs(writer, route, route.getInputs());
	}

	/**
	 * Prints a single node of a Camel route in Prolog representation.
	 * 
	 * @param indent
	 * @param writer
	 * @param current
	 * @param preds
	 * @return
	 * @throws IOException 
	 */
	private List<ProcessorDefinition<?>> printNode(Writer writer, ProcessorDefinition<?> current, List<OptionalIdentifiedDefinition<?>> preds) throws IOException {
		writer.write("node(node"+current.getIndex() + ").\n");
		writer.write("has_action(node"+current.getIndex() + ", \"" + current.getLabel() + "\").\n");
		for (OptionalIdentifiedDefinition<?> p : preds) {
			if (p instanceof FromDefinition) {
				writer.write("succ(input" + ((FromDefinition) p).getId() +", node" + current.getIndex() + ").\n");				
			} else if (p instanceof ProcessorDefinition) {
				writer.write("succ(node" + ((ProcessorDefinition<?>) p).getIndex() +", node" + current.getIndex() + ").\n");				
			}
		}

		// predecessor of next recursion is the current node 
		List<ProcessorDefinition<?>> newPreds = new ArrayList<>();
		newPreds.add(current);
		for (ProcessorDefinition<?> out : current.getOutputs()) {
			
			// if this is a ChoiceDefinition, there is no link between its WhereDefinitions.
			ArrayList<OptionalIdentifiedDefinition<?>> myPreds = new ArrayList<>();
			if (current instanceof ChoiceDefinition) {
				myPreds.add(current);
			} else {
				myPreds.addAll(newPreds);
			}
			
			// Recursion ...
			List<ProcessorDefinition<?>> p = printNode(writer, out, myPreds);
			
			// Predecessors of a ChoiceDefinition are all last stmts of its Where- and OtherwiseDefinitions
			if (current instanceof ChoiceDefinition) {
				newPreds.addAll(p);
			} else {
				newPreds.clear(); newPreds.addAll(p);
			}
		}
		
		return newPreds;
	}

	/**
	 * Prints a single FromDefinition (= a route entry point) in Prolog representation.
	 * @throws IOException 
	 */
	private void printInputs(Writer writer, RouteDefinition route, List<FromDefinition> inputs) throws IOException {
		int counter = 0;
		for (FromDefinition i : inputs) {
			// Make sure every input node has a unique id
			if (i.getId()==null) {
				i.setCustomId(true);
				i.setId(String.valueOf(counter));
			}			
			String nodeName = "input"+i.getId();
			writer.write("node("+nodeName+").\n");
			writer.write("has_action("+nodeName+", \"" + i.getLabel() + "\").\n");
			
			ArrayList<OptionalIdentifiedDefinition<?>> preds = new ArrayList<>();
			preds.add(i);

			for (ProcessorDefinition<?> next : route.getOutputs()) {
				printNode(writer, next, preds);
				preds.clear();
				preds.add(next);
			}
		}
	}
}
