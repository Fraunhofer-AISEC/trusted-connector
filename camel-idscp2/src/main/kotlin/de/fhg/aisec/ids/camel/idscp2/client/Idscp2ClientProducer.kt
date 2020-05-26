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

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import org.apache.camel.Exchange
import org.apache.camel.support.DefaultProducer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * The IDSCP2 server producer.
 * Sends each message to all clients connected to this server endpoint.
 */
class Idscp2ClientProducer(private val endpoint: Idscp2ClientEndpoint) : DefaultProducer(endpoint) {
    private val connectionFuture = CompletableFuture<Idscp2Connection>()

    override fun process(exchange: Exchange) {
        val message = exchange.getIn()
        val type = message.getHeader("idscp2.type", String::class.java)
        val body = message.getBody(ByteArray::class.java)
        connectionFuture.get().let {
            it.unlockMessaging()
            it.send(type, body)
        }
    }

    override fun doStart() {
        super.doStart()
        endpoint.makeConnection(connectionFuture)
    }

    public override fun doStop() {
        if (connectionFuture.isDone) {
            val connection = connectionFuture.get()
            LOG.debug("Stopping IDSCP2 client connection {}...", connection.id)
            connection.close()
        } else {
            LOG.debug("Canceling IDSCP2 client connection...")
            connectionFuture.cancel(true)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientProducer::class.java)
    }
}