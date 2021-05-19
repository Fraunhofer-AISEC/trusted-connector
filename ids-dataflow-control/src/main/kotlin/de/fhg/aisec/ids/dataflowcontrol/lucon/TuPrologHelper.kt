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
import java.util.Spliterator
import java.util.Spliterators
import java.util.stream.Stream
import java.util.stream.StreamSupport

object TuPrologHelper {

    fun escape(s: String?): String {
        if (s == null) {
            return ""
        }
        val sb = StringBuilder()
        sb.append('\'')
        val charLength = s.length
        for (i in 0 until charLength) {
            val c = s[i]
            sb.append(if (c == '\'') "''" else c)
        }
        sb.append('\'')
        return sb.toString()
    }

    fun listStream(list: Term?): Stream<out Term?> {
        if (list == null) {
            return Stream.empty()
        }
        require(list.isList) { "Not a tuProlog list" }
        val listIterator = (list as Struct).listIterator()
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(listIterator, Spliterator.ORDERED),
            false
        )
    }

    fun unquote(s: String): String {
        return if (s.length > 2 && s[0] == '\'' && s[s.length - 1] == '\'') {
            s.substring(1, s.length - 1)
        } else if (s.length == 2 && "''" == s) {
            ""
        } else {
            s
        }
    }
}
