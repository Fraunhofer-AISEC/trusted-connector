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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PrologPrinter {

  /**
   * Prints a single Camel route in Prolog representation.
   *
   * @param writer
   * @param route
   * @throws IOException
   */
  public void printSingleRoute(@Nullable Writer writer, @Nullable RouteDefinition route)
      throws IOException {
    if (writer == null || route == null) {
      return;
    }

    // Print route entry points
    printInputs(writer, route, route.getInputs());
  }

  /**
   * Prints a single node of a Camel route in Prolog representation.
   *
   * @param writer
   * @param current
   * @param preds
   * @return
   * @throws IOException
   */
  private List<ProcessorDefinition<?>> printNode(
      Writer writer, ProcessorDefinition<?> current, List<OptionalIdentifiedDefinition<?>> preds)
      throws IOException {
    for (OptionalIdentifiedDefinition<?> p : preds) {
      writer.write("succ(" + p.getId() + ", " + current.getId() + ").\n");
    }
    writer.write("stmt(" + current.getId() + ").\n");
    writer.write("has_action(" + current.getId() + ", \"" + current.getLabel() + "\").\n");

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
      List<ProcessorDefinition<?>> p = printNode(writer, out, myPreds);

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
  private void printInputs(Writer writer, RouteDefinition route, List<FromDefinition> inputs)
      throws IOException {
    AtomicInteger counter = new AtomicInteger(0);
    for (FromDefinition i : inputs) {
      // Make sure every input node has a unique id
      if (i.getId() == null) {
        i.setCustomId(true);
        i.setId("input" + counter.incrementAndGet());
      }
      writer.write("stmt(" + i.getId() + ").\n");
      writer.write("entrynode(" + i.getId() + ").\n");
      writer.write("has_action(" + i.getId() + ", \"" + i.getLabel() + "\").\n");

      OptionalIdentifiedDefinition<?> prev = i;
      for (ProcessorDefinition<?> next : route.getOutputs()) {
        printNode(writer, next, Collections.singletonList(prev));
        prev = next;
      }
    }
  }
}
