/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole.api.helper

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

internal class StreamGobbler(var `is`: InputStream, var out: OutputStream?) : Thread() {
    override fun run() {
        try {
            InputStreamReader(`is`, StandardCharsets.UTF_8).use { isr ->
                BufferedReader(isr).use { br ->
                    out?.let {
                        BufferedWriter(OutputStreamWriter(it, StandardCharsets.UTF_8)).use { bw ->
                            br.lines().forEach { line ->
                                bw.write(line)
                                bw.newLine()
                            }
                        }
                    } ?: {
                        br.lines().forEach { /* no-op */ }
                    }
                }
            }
        } catch (ioe: IOException) {
            LOG.error(ioe.message, ioe)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StreamGobbler::class.java)
    }
}
