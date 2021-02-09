/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.aisec.ids.camel.idscp2.client

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.UsageControlMaps
import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fraunhofer.iais.eis.Message
import org.apache.camel.Exchange
import org.apache.camel.support.DefaultProducer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The IDSCP2 client producer.
 * Sends a message to the server connected to this client endpoint.
 */
class Idscp2ClientProducer(private val endpoint: Idscp2ClientEndpoint) : DefaultProducer(endpoint) {
    private lateinit var connectionFuture: CompletableFuture<AppLayerConnection>
    private lateinit var reentrantLock: ReentrantLock

    override fun process(exchange: Exchange) {
        exchange.message.let { message ->
            val header = message.getHeader(IDSCP2_HEADER)
            val body = message.getBody(ByteArray::class.java)
            if (header != null || body != null) {
                for (t in 1..endpoint.maxRetries) {
                    try {
                        // If connectionFuture completed exceptionally, recreate Connection
                        if (connectionFuture.isCompletedExceptionally) {
                            connectionFuture = endpoint.makeConnection()
                                .also { c -> c.thenAccept { it.unlockMessaging() } }
                        }
                        val connection = connectionFuture.get()
                        if (endpoint.awaitResponse) {
                            val condition = reentrantLock.newCondition()
                            val responseHandler = { responseHeader: Any?, responsePayload: ByteArray? ->
                                message.setHeader(IDSCP2_HEADER, responseHeader)
                                message.body = responsePayload
                                reentrantLock.withLock {
                                    condition.signal()
                                }
                            }
                            reentrantLock.withLock {
                                if (endpoint.useIdsMessages) {
                                    // Response might require UC protection, so register exchange if not yet registered
                                    UsageControlMaps.setExchangeConnection(exchange, connection)
                                    connection.addIdsMessageListener { _, responseHeader, responsePayload ->
                                        responseHandler(responseHeader, responsePayload)
                                    }
                                    connection.sendIdsMessage(header?.let { header as Message }, body)
                                } else {
                                    connection.addGenericMessageListener { _, responseHeader, responsePayload ->
                                        responseHandler(responseHeader, responsePayload)
                                    }
                                    connection.sendGenericMessage(header?.toString(), body)
                                }
                                condition.await()
                            }
                        } else {
                            if (endpoint.useIdsMessages) {
                                connection.sendIdsMessage(header?.let { header as Message }, body)
                            } else {
                                connection.sendGenericMessage(header?.toString(), body)
                            }
                        }
                        return
                    } catch (x: Exception) {
                        if (endpoint.maxRetries == t) {
                            LOG.error("Massage delivery failed finally, aborting exchange...")
                            exchange.setException(x)
                        } else {
                            LOG.warn("Message delivery failed in attempt $t, " +
                                    "reset connection and retry after ${endpoint.retryDelayMs} ms...", x)
                            endpoint.releaseConnection(connectionFuture)
                            connectionFuture = endpoint.makeConnection()
                                .also { c -> c.thenAccept { it.unlockMessaging() } }
                            Thread.sleep(endpoint.retryDelayMs)
                        }
                    }
                }
            }
        }
    }

    override fun doStart() {
        super.doStart()
        if (endpoint.awaitResponse) {
            reentrantLock = ReentrantLock()
        }
        connectionFuture = endpoint.makeConnection().also { c ->
            // Unlock messaging immediately after obtaining connection
            c.thenAccept { it.unlockMessaging() }.exceptionally {
                LOG.warn("Could not connect to Server ${endpoint.endpointUri}, delaying connect until first message...")
                null
            }
        }
    }

    public override fun doStop() {
        LOG.debug("Stopping/releasing IDSCP2 client producer connection {}...",
                if (connectionFuture.isDone) connectionFuture.get().id else "<pending>")
        endpoint.releaseConnection(connectionFuture)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientProducer::class.java)
    }
}