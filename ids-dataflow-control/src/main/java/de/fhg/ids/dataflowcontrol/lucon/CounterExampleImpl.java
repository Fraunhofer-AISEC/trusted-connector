/*-
 * ========================LICENSE_START=================================
 * LUCON Data Flow Policy Engine
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
package de.fhg.ids.dataflowcontrol.lucon;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import de.fhg.aisec.ids.api.router.CounterExample;

import java.util.Iterator;
import java.util.LinkedList;

public class CounterExampleImpl extends CounterExample {

    public CounterExampleImpl(Term term) {
        Iterator<? extends Term> traceIterator = ((Struct) term).listIterator();
        LinkedList<String> steps = new LinkedList<>();
        // process explanation
        Iterator<? extends Term> reasonIterator = ((Struct) traceIterator.next()).listIterator();
        StringBuilder sb = new StringBuilder().append("Service ").append(reasonIterator.next().toString())
                .append(" may receive messages");
        Term explanation = reasonIterator.next();
////        if (explanation.isList()) {
//            sb.append(" labeled [");
//            appendCSList(sb, explanation);
//            sb.append("]");
////        }
        sb.append(", which is forbidden by rule \"").append(reasonIterator.next().toString()).append("\".");
        this.setExplanation(sb.toString());
        // process steps and prepend them to list (inverse trace to get the right order)
        traceIterator.forEachRemaining(t -> steps.addFirst(termToStep(t)));
        this.setSteps(steps);
    }

    public static String termToStep(Term t) {
        Struct traceEntry = (Struct) t;
        StringBuilder sb = new StringBuilder();
        // node name is the head of the list
        String node = traceEntry.listHead().toString();
        sb.append(node);
        // the label list is the new head of the remaining list (tail)
        Term labelList = traceEntry.listTail().listHead();
        if (!labelList.isEmptyList()) {
            sb.append(" receives message labelled ");
            appendCSList(sb, labelList);
            return sb.toString();
        } else {
            return sb.append(" receives message without labels").toString();
        }
    }

    public static void appendCSList(StringBuilder sb, Term l) {
        if (l.isList() && !l.isEmptyList()) {
            Iterator<? extends Term> listIterator = ((Struct) l).listIterator();
            // add first element
            sb.append(listIterator.next().toString());
            // add remaining elements
            listIterator.forEachRemaining(lt -> sb.append(", ").append(lt.toString()));
        }
    }

}
