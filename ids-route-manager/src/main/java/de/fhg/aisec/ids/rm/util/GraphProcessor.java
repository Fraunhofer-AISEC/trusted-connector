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
package de.fhg.aisec.ids.rm.util;

import de.fhg.aisec.ids.api.router.graph.Edge;
import de.fhg.aisec.ids.api.router.graph.GraphData;
import de.fhg.aisec.ids.api.router.graph.Node;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.camel.model.*;

public class GraphProcessor {

  /**
   * Prints a single Camel route in Prolog representation.
   *
   * @param route The route to be transformed
   */
  public static GraphData processRoute(RouteDefinition route) {
    GraphData gd = new GraphData();
    // Print route entry points
    processInputs(gd, route, route.getInputs());
    return gd;
  }

  /**
   * Prints a single node of a Camel route in Prolog representation.
   *
   * @param graphData
   * @param current
   * @param preds
   * @return
   * @throws IOException
   */
  private static List<ProcessorDefinition<?>> processNode(
      GraphData graphData,
      ProcessorDefinition<?> current,
      List<OptionalIdentifiedDefinition<?>> preds) {
    for (OptionalIdentifiedDefinition<?> p : preds) {
      graphData.addEdge(new Edge(p.getId(), current.getId()));
    }
    graphData.addNode(
        new Node(
            current.getId(),
            current.getLabel(),
            (current instanceof ChoiceDefinition) ? Node.NodeType.ChoiceNode : Node.NodeType.Node));

    // predecessor of next recursion is the current node
    List<ProcessorDefinition<?>> newPreds = new ArrayList<>();
    newPreds.add(current);
    for (ProcessorDefinition<?> out : current.getOutputs()) {
      // if this is a ChoiceDefinition, there is no link between its WhereDefinitions.
      ArrayList<OptionalIdentifiedDefinition<?>> myPreds = new ArrayList<>();
      if (current instanceof ChoiceDefinition) {
        myPreds.add(current);
      } else {
        // @TODO: Looks somewhat strange... is this correct?
        myPreds.addAll(newPreds);
      }

      // Recursion ...
      List<ProcessorDefinition<?>> p = processNode(graphData, out, myPreds);

      // Predecessors of a ChoiceDefinition are all last stmts of its Where- and
      // OtherwiseDefinitions
      if (current instanceof ChoiceDefinition) {
        newPreds.addAll(p);
      } else {
        newPreds.clear();
        newPreds.addAll(p);
      }
    }

    return newPreds;
  }

  /**
   * Prints a single FromDefinition (= a route entry point) in Prolog representation.
   *
   * @throws IOException
   */
  private static void processInputs(
      GraphData graphData, RouteDefinition route, List<FromDefinition> inputs) {
    AtomicInteger counter = new AtomicInteger(0);
    for (FromDefinition i : inputs) {
      // Make sure every input node has a unique id
      if (i.getId() == null) {
        i.setCustomId(true);
        i.setId("input" + counter);
      }
      graphData.addNode(new Node(i.getId(), i.getLabel(), Node.NodeType.EntryNode));

      OptionalIdentifiedDefinition<?> prev = i;
      for (ProcessorDefinition<?> next : route.getOutputs()) {
        processNode(graphData, next, Collections.singletonList(prev));
        prev = next;
      }
    }
  }
}
