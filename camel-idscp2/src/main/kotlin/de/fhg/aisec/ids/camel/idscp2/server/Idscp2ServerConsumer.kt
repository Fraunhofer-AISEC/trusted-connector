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
package de.fhg.aisec.ids.camel.idscp2.server

import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.app_layer.listeners.GenericMessageListener
import org.apache.camel.Processor
import org.apache.camel.support.DefaultConsumer
import org.slf4j.LoggerFactory

/**
 * The IDSCP2 server consumer.
 */
class Idscp2ServerConsumer(private val endpoint: Idscp2ServerEndpoint, processor: Processor) :
        DefaultConsumer(endpoint, processor), GenericMessageListener {
    override fun doStart() {
        super.doStart()
        endpoint.addConsumer(this)
    }

    override fun doStop() {
        endpoint.removeConsumer(this)
        super.doStop()
    }

    override fun onMessage(connection: AppLayerConnection, header: String, payload: ByteArray) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Idscp2ServerConsumer received GenericMessage with header:\n{}", header)
        }
        val exchange = endpoint.createExchange()
        try {
            createUoW(exchange)
            // Set relevant information
            exchange.getIn().setHeader("idscp2.type", header)
            exchange.getIn().setBody(payload, ByteArray::class.java)
            // Do processing
            processor.process(exchange)
            // Handle response
            val response = exchange.message
            val responseType = response.getHeader("idscp2.type", String::class.java)
            if (response.body != null && responseType != null) {
                connection.sendGenericMessage(responseType, response.getBody(ByteArray::class.java))
            }
        } catch (e: Exception) {
            LOG.error("Error in Idscp2ServerConsumer.onMessage()", e)
        } finally {
            doneUoW(exchange)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ServerConsumer::class.java)
    }

}