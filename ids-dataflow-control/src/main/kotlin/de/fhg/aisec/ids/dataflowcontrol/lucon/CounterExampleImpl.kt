/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
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
package de.fhg.aisec.ids.dataflowcontrol.lucon

import alice.tuprolog.Struct
import alice.tuprolog.Term
import de.fhg.aisec.ids.api.router.CounterExample
import java.util.LinkedList

class CounterExampleImpl(term: Term) : CounterExample() {
    init {
        val traceIterator = (term as Struct).listIterator()
        val steps = LinkedList<String>()
        // process explanation
        val reasonIterator = (traceIterator.next() as Struct).listIterator()
        val sb =
            StringBuilder()
                .append("Service ")
                .append(reasonIterator.next().toString())
                .append(" may receive messages")
        reasonIterator.next()
        //        val explanation = reasonIterator.next()
        //        if (explanation.isList) {
        //            sb.append(" labeled [")
        //            appendCSList(sb, explanation)
        //            sb.append("]")
        //        }
        sb.append(", which is forbidden by rule \"")
            .append(reasonIterator.next().toString())
            .append("\".")
        this.explanation = sb.toString()
        // process steps and prepend them to list (inverse trace to get the right order)
        traceIterator.forEachRemaining { t: Term? -> steps.addFirst(termToStep(t)) }
        this.steps = steps
    }

    companion object {
        fun termToStep(t: Term?): String? {
            if (t == null) {
                return null
            }
            val traceEntry = t as Struct
            val sb = StringBuilder()
            // node name is the head of the list
            val node = traceEntry.listHead().toString()
            sb.append(node)
            // the label list is the new head of the remaining list (tail)
            val labelList = traceEntry.listTail().listHead()
            return if (!labelList.isEmptyList) {
                sb.append(" receives message labelled ")
                appendCSList(sb, labelList)
                sb.toString()
            } else {
                sb.append(" receives message without labels").toString()
            }
        }

        private fun appendCSList(
            sb: StringBuilder?,
            l: Term?
        ) {
            if (sb == null || l == null) {
                return
            }
            if (l.isList && !l.isEmptyList) {
                val listIterator = (l as Struct).listIterator()
                // add first element
                sb.append(listIterator.next().toString())
                // add remaining elements
                listIterator.forEachRemaining { lt: Term -> sb.append(", ").append(lt.toString()) }
            }
        }
    }
}
