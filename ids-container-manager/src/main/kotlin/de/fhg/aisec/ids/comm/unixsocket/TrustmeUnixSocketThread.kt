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

import jnr.enxio.channels.NativeSelectorProvider
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.Collections
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class TrustmeUnixSocketThread(private val socket: String) : Runnable {
    // The selector we'll be monitoring
    private val selector: Selector
    private val lengthBuffer = ByteBuffer.allocate(4)

    // A list of PendingChange instances
    private val pendingChanges: MutableList<ChangeRequest> = LinkedList()

    // Maps a UnixSocketChannel to a list of ByteBuffer instances
    private val pendingData: MutableMap<UnixSocketChannel, MutableList<ByteBuffer>> = HashMap()

    // Maps a UnixSocketChannel to a UnixSocketResponseHandler
    private val rspHandlers =
        Collections.synchronizedMap(HashMap<UnixSocketChannel, TrustmeUnixSocketResponseHandler>())

    // send a protobuf message to the unix socket
    @Throws(IOException::class, InterruptedException::class)
    fun sendWithHeader(
        data: ByteArray,
        handler: TrustmeUnixSocketResponseHandler?
    ) {
        send(data, handler, true)
    }

    // send some data to the unix socket
    @Throws(IOException::class, InterruptedException::class)
    fun send(
        data: ByteArray,
        handler: TrustmeUnixSocketResponseHandler?,
        withLengthHeader: Boolean
    ) {
        LOG.debug("writing protobuf to socket")
        var result = data
        // if message has to be sent with length header
        if (withLengthHeader) {
            // then append the length of the message
            val length = data.size
            // in the first 4 bytes
            lengthBuffer.clear()
            val bb = ByteBuffer.allocate(4 + length)
            bb.put(lengthBuffer.putInt(length).array())
            bb.put(data)
            result = bb.array()
        }
        val socket = initiateConnection()
        // Register the response handler
        rspHandlers[socket] = handler
        // And queue the data we want written
        synchronized(pendingData) {
            val queue = pendingData.computeIfAbsent(socket) { ArrayList() }
            queue.add(ByteBuffer.wrap(result))
        }

        // Finally, wake up our selecting thread so it can make the required changes
        selector.wakeup()
    }

    // thread run method
    override fun run() {
        while (!Thread.interrupted()) {
            try {
                // Process any pending changes
                synchronized(pendingChanges) {
                    for (change in pendingChanges) {
                        when (change.type) {
                            ChangeRequest.CHANGEOPS -> {
                                val key = change.channel.keyFor(selector)
                                key.interestOps(change.ops)
                            }
                            ChangeRequest.REGISTER -> change.channel.register(selector, change.ops)
                            else -> LOG.warn("Unknown ChangeRequest type {}", change.type)
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
                    if (key.isConnectable) {
                        finishConnection(key)
                    }
                    if (key.isReadable) {
                        read(key)
                    } else if (key.isWritable) {
                        write(key)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // read send some data from the unix socket
    @Throws(IOException::class)
    private fun read(key: SelectionKey) {
        LOG.debug("reading protobuf from socket")
        val channel = getChannel(key)
        val length = readMessageLength(key, channel)
        if (length == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and
            // cancel the channel.
            LOG.debug("Closing channel because length = -1")
            key.channel().close()
            key.cancel()
            return
        }
        val messageBuffer = ByteBuffer.allocate(length)
        messageBuffer.clear()
        var numRead = 0
        var totalRead = 0
        try {
            while (totalRead < length) {
                numRead = channel.read(messageBuffer)
                totalRead += numRead
                LOG.debug("read {} bytes of protobuf from socket, {} in total", numRead, totalRead)
            }
        } catch (e: IOException) {
            // The remote forcibly closed the connection, cancel the selection key and close
            // the channel.
            LOG.debug("error while reading from socket", e)
            key.cancel()
            channel.close()
            return
        }
        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
            // channel.
            LOG.debug("Closing channel because numRead = -1")
            key.channel().close()
            key.cancel()
            return
        }

        // Handle the response
        handleResponse(channel, messageBuffer.array())
    }

    @Throws(IOException::class)
    private fun readMessageLength(
        key: SelectionKey,
        channel: UnixSocketChannel
    ): Int {
        // Clear out our read buffer so it's ready for new data
        lengthBuffer.clear()

        // Attempt to read off the channel
        val length: Int =
            try {
                val numRead = channel.read(lengthBuffer)
                if (numRead == 4) {
                    BigInteger(lengthBuffer.array()).toInt()
                } else {
                    -1
                }
            } catch (e: IOException) {
                // The remote forcibly closed the connection, cancel the selection key and close
                // the channel.
                LOG.debug("error while reading message length from socket", e)
                key.cancel()
                channel.close()
                return -1
            }
        LOG.debug("read message from UNIX socket with length $length")
        return length
    }

    @Throws(IOException::class)
    private fun handleResponse(
        socketChannel: UnixSocketChannel,
        data: ByteArray
    ) {
        // Make a correctly sized copy of the data before handing it
        // to the client
        val rspData = ByteArray(data.size)
        System.arraycopy(data, 0, rspData, 0, data.size)

        // Look up the handler for this channel
        val handler = rspHandlers[socketChannel]

        // And pass the response to it
        if (handler!!.handleResponse(rspData)) {
            LOG.debug("Handler done, close channel")
            // The handler has seen enough, close the connection
            socketChannel.close()
            socketChannel.keyFor(selector).cancel()
        }
    }

    @Throws(IOException::class)
    private fun write(key: SelectionKey) {
        val channel = getChannel(key)
        synchronized(pendingData) {
            val queue = pendingData[channel]!!

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
                // We wrote away all data, so we're no longer interested in writing on this
                // socket. Switch back to waiting for data.
                key.interestOps(SelectionKey.OP_READ)
            }
        }
    }

    private fun finishConnection(key: SelectionKey) {
        val channel = getChannel(key)

        // Finish the connection. If the connection operation failed this will raise an
        // IOException.
        try {
            println(channel.finishConnect())
        } catch (e: IOException) {
            // Cancel the channel's registration with our selector
            LOG.debug(e.message, e)
            key.cancel()
            return
        }

        // Register an interest in writing on this channel
        // this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS,
        // SelectionKey.OP_WRITE));
        // key.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup()
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun initiateConnection(): UnixSocketChannel {
        // open the socket address
        var socketFile = File(socket)
        // Try to open socket file 10 times
        var retries = 0
        while (!socketFile.absoluteFile.exists() && retries < 10) {
            ++retries
            TimeUnit.MILLISECONDS.sleep(500L)
            socketFile = File(socket)
            if (retries < 10) {
                LOG.debug(
                    String.format(
                        "error: socket \"%s\" does not exist after %s retry.",
                        socketFile.absolutePath,
                        retries
                    )
                )
            }
        }
        val address = UnixSocketAddress(socketFile.absoluteFile)
        val channel = UnixSocketChannel.open(address)
        channel.configureBlocking(false)
        // synchronize pending changes
        synchronized(pendingChanges) {
            pendingChanges.add(
                ChangeRequest(
                    channel,
                    ChangeRequest.REGISTER,
                    SelectionKey.OP_CONNECT or SelectionKey.OP_WRITE
                )
            )
        }
        return channel
    }

    // initialize the selector
    @Throws(IOException::class)
    private fun initSelector(): Selector {
        return NativeSelectorProvider.getInstance().openSelector()
    }

    // get the channel
    private fun getChannel(k: SelectionKey): UnixSocketChannel {
        return k.channel() as UnixSocketChannel
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TrustmeUnixSocketThread::class.java)
    }

    // constructor setting another socket address
    init {
        selector = initSelector()
    }
}
