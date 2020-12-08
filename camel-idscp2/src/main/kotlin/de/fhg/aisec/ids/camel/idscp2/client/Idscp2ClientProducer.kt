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
            }
        }
    }

    override fun doStart() {
        super.doStart()
        if (endpoint.awaitResponse) {
            reentrantLock = ReentrantLock()
        }
        connectionFuture = endpoint.makeConnection()
        // Unlock messaging immediately after obtaining connection
        connectionFuture.thenAccept { it.unlockMessaging() }
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