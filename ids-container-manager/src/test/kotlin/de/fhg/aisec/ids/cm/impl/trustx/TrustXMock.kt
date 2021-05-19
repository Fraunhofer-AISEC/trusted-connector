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

import de.fhg.aisec.ids.comm.unixsocket.ChangeRequest
import jnr.enxio.channels.NativeSelectorProvider
import jnr.unixsocket.UnixServerSocketChannel
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.LinkedList

class TrustXMock(private var socket: String, private var handler: TrustXMockHandler) : Runnable {
    private var channel: UnixServerSocketChannel? = null

    // The selector we'll be monitoring
    private val selector: Selector = initSelector()

    // The buffer into which we'll read data when it's available
    private val readBuffer = ByteBuffer.allocate(1024 * 1024)

    // A list of PendingChange instances
    private val pendingChanges: MutableList<ChangeRequest> = LinkedList()

    // Maps a UnixSocketChannel to a list of ByteBuffer instances
    private val pendingData: MutableMap<UnixSocketChannel?, MutableList<ByteBuffer>> = HashMap()

    // send a protobuf message to the unix socket
    fun send(socket: UnixSocketChannel?, data: ByteArray?) {
        synchronized(pendingChanges) {
            pendingChanges.add(
                ChangeRequest(socket!!, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE)
            )
            // And queue the data we want written
            synchronized(pendingData) {
                val queue = pendingData.computeIfAbsent(socket) { ArrayList() }
                queue.add(ByteBuffer.wrap(data))
            }
            // Finally, wake up our selecting thread so it can make the required changes
            selector.wakeup()
        }
    }

    // thread run method
    override fun run() {
        while (!Thread.interrupted()) {
            try {
                // Process any pending changes
                synchronized(pendingChanges) {
                    for (change in pendingChanges) {
                        if (change.type == ChangeRequest.CHANGEOPS) {
                            val key = change.channel.keyFor(selector)
                            key.interestOps(change.ops)
                        }
                    }
                    pendingChanges.clear()
                }

                // Wait for an event on one of the registered channels
                selector.select()

                // Iterate over the set of keys for which events are available
                val selectedKeys = selector.selectedKeys().iterator()
                while (selectedKeys.hasNext()) {
                    val key = selectedKeys.next()
                    selectedKeys.remove()
                    if (!key.isValid) {
                        continue
                    }
                    // Check what event is available and deal with it
                    when {
                        key.isAcceptable -> {
                            accept()
                        }
                        key.isReadable -> {
                            read(key)
                        }
                        key.isWritable -> {
                            write(key)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun accept() {
        // Accept the connection and make it non-blocking
        val client = channel!!.accept()
        client.configureBlocking(false)

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        client.register(selector, SelectionKey.OP_READ)
    }

    // read send some data from the unix socket
    @Throws(IOException::class)
    private fun read(key: SelectionKey) {
        val channel = getChannel(key)

        // Clear out our read buffer so it's ready for new data
        readBuffer.clear()

        // Attempt to read off the channel
        val numRead: Int = try {
            channel.read(readBuffer)
        } catch (e: IOException) {
            // The remote forcibly closed the connection, cancel the selection key and close the channel.
            key.cancel()
            channel.close()
            return
        }
        LOG.debug("bytes read: $numRead")
        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
            // channel.
            key.channel().close()
            key.cancel()
            return
        }
        handler.handleResponse(this, channel, readBuffer.array(), numRead)
    }

    @Throws(IOException::class)
    private fun write(key: SelectionKey) {
        val channel = getChannel(key)
        synchronized(pendingData) {
            val queue: MutableList<ByteBuffer> = pendingData[channel]!!

            // Write until there's not more data
            while (queue.isNotEmpty()) {
                val buf = queue[0]
                channel.write(buf)
                if (buf.remaining() > 0) {
                    // ... or the socket's buffer fills up
                    break
                }
                queue.removeAt(0)
            }
            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested in writing on this socket. Switch
                // back to waiting for data.
                key.interestOps(SelectionKey.OP_READ)
            }
        }
    }

    // initialize the selector
    @Throws(IOException::class)
    private fun initSelector(): Selector {
        val socketSelector: Selector = NativeSelectorProvider.getInstance().openSelector()
        val socketFile = File(socket)
        socketFile.delete()
        socketFile.deleteOnExit()
        val address = UnixSocketAddress(socketFile.absoluteFile)
        channel = UnixServerSocketChannel.open().apply {
            configureBlocking(false)
            socket().bind(address)
            register(socketSelector, SelectionKey.OP_ACCEPT)
        }
        return socketSelector
    }

    // get the channel
    private fun getChannel(k: SelectionKey): UnixSocketChannel {
        return k.channel() as UnixSocketChannel
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TrustXMock::class.java)
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val handler = TrustXMockHandler()
                Thread(handler).start()
                Thread(TrustXMock("src/test/socket/trustme.sock", handler)).start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
