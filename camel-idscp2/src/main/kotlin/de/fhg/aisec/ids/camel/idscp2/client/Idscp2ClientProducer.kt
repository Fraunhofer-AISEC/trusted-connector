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

import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import org.apache.camel.Exchange
import org.apache.camel.support.DefaultProducer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * The IDSCP2 server producer.
 * Sends each message to all clients connected to this server endpoint.
 */
class Idscp2ClientProducer(private val endpoint: Idscp2ClientEndpoint) : DefaultProducer(endpoint) {
    private lateinit var connectionFuture: CompletableFuture<AppLayerConnection>

    override fun process(exchange: Exchange) {
        val message = exchange.getIn()
        val type = message.getHeader("idscp2-header", String::class.java)
        val body = message.getBody(ByteArray::class.java)
        connectionFuture.get().sendGenericMessage(type, body)
    }

    override fun doStart() {
        super.doStart()
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