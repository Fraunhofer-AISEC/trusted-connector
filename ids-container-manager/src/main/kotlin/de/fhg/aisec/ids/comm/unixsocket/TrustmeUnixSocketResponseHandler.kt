/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.aisec.ids.comm.unixsocket

import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TrustmeUnixSocketResponseHandler {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private val LOG = LoggerFactory.getLogger(TrustmeUnixSocketResponseHandler::class.java)
    private var rsp: ByteArray? = null

    fun handleResponse(rsp: ByteArray): Boolean {
        lock.withLock {
            this.rsp = rsp.clone()
            condition.signal()
            return true
        }
    }

    fun waitForResponse(): ByteArray {
        lock.withLock {
            while (rsp == null) {
                try {
                    condition.await()
                } catch (e: InterruptedException) {
                    LOG.error(e.message, e)
                }
            }
            val result = rsp!!
            LOG.debug("received response byte length: {}", result.size)
            rsp = null
            return result
        }
    }
}
