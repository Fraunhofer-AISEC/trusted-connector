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
package de.fhg.aisec.ids.cm.impl.trustx

import jnr.unixsocket.UnixSocketChannel
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TrustXMockHandler : Runnable {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val queue: MutableList<ServerDataEvent> = LinkedList()

    override fun run() {
        var dataEvent: ServerDataEvent
        while (true) {
            // Wait for data to become available
            lock.withLock {
                while (queue.isEmpty()) {
                    try {
                        condition.await()
                    } catch (ignored: InterruptedException) {
                    }
                }
                dataEvent = queue.removeAt(0)
            }

            // Print
            println(String(dataEvent.data))
            dataEvent.server.send(dataEvent.socket, dataEvent.data)
        }
    }

    fun handleResponse(server: TrustXMock, socket: UnixSocketChannel, data: ByteArray, count: Int) {
        val dataCopy = ByteArray(count)
        System.arraycopy(data, 0, dataCopy, 0, count)
        lock.withLock {
            queue.add(ServerDataEvent(server, socket, dataCopy))
            condition.signal()
        }
    }
}
