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
import de.fhg.aisec.ids.idscp2.app_layer.listeners.GenericMessageListener
import de.fhg.aisec.ids.idscp2.app_layer.listeners.IdsMessageListener
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2ConnectionListener
import de.fraunhofer.iais.eis.Message
import org.apache.camel.Processor
import org.apache.camel.support.DefaultConsumer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * The IDSCP2 client consumer.
 */
class Idscp2ClientConsumer(private val endpoint: Idscp2ClientEndpoint, processor: Processor) :
        DefaultConsumer(endpoint, processor), GenericMessageListener, IdsMessageListener {
    private lateinit var connectionFuture: CompletableFuture<AppLayerConnection>

    override fun doStart() {
        super.doStart()
        connectionFuture = endpoint.makeConnection()
        connectionFuture.thenAccept {
            if (endpoint.useIdsMessages) {
                it.addIdsMessageListener(this)
            } else {
                it.addGenericMessageListener(this)
            }
            // Handle connection errors and closing
            it.addConnectionListener(object : Idscp2ConnectionListener {
                override fun onError(t: Throwable) {
                    LOG.error("Error in Idscp2ClientConsumer connection", t)
                }
                override fun onClose() {
                    stop()
                }
            })
            it.unlockMessaging()
        }
    }

    public override fun doStop() {
        LOG.debug("Stopping/releasing IDSCP2 client consumer connection {}...",
                if (connectionFuture.isDone) connectionFuture.get().id else "<pending>")
        endpoint.releaseConnection(connectionFuture)
    }

    override fun onMessage(connection: AppLayerConnection, header: Message?, payload: ByteArray?) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Idscp2ClientConsumer received IdsMessage with header:\n{}", header)
        }
        onMessage(connection, header as Any, payload)
    }

    override fun onMessage(connection: AppLayerConnection, header: String?, payload: ByteArray?) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Idscp2ClientConsumer received GenericMessage with header:\n{}", header)
        }
        onMessage(connection, header as Any, payload)
    }

    private fun onMessage(connection: AppLayerConnection, header: Any?, payload: ByteArray?) {
        val exchange = endpoint.createExchange()
        if (endpoint.useIdsMessages) {
            UsageControlMaps.setExchangeConnection(exchange, connection)
        }
        try {
            createUoW(exchange)
            // Set relevant information
            exchange.message.let {
                it.setHeader(IDSCP2_HEADER, header)
                it.setBody(payload, ByteArray::class.java)
            }
            // Do processing
            processor.process(exchange)
            // Handle response
            exchange.message.let {
                val responseHeader = it.getHeader(IDSCP2_HEADER)
                val responseBody = it.getBody(ByteArray::class.java)
                if (responseBody != null || responseHeader != null) {
                    if (endpoint.useIdsMessages) {
                        connection.sendIdsMessage(responseHeader?.let { responseHeader as Message }, responseBody)
                    } else {
                        connection.sendGenericMessage(responseHeader?.toString(), responseBody)
                    }
                }
            }
        } finally {
            doneUoW(exchange)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientConsumer::class.java)
    }

}