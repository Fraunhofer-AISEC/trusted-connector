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
import java.io.IOException
import java.io.OutputStream

class ProcessExecutor {
    @Throws(InterruptedException::class, IOException::class)
    fun execute(
        cmd: Array<String>?,
        stdout: OutputStream?,
        stderr: OutputStream?
    ): Int {
        val rt = Runtime.getRuntime()
        val proc = rt.exec(cmd)
        val errorGobbler = StreamGobbler(proc.errorStream, stderr)
        val outputGobbler = StreamGobbler(proc.inputStream, stdout)
        errorGobbler.start()
        outputGobbler.start()
        return proc.waitFor()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ProcessExecutor::class.java)
    }
}
