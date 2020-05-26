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
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageListener
import org.apache.camel.Processor
import org.apache.camel.support.DefaultConsumer
import org.slf4j.LoggerFactory

/**
 * The IDSCP2 server consumer.
 */
class Idscp2ClientConsumer(private val endpoint: Idscp2ClientEndpoint, processor: Processor) :
        DefaultConsumer(endpoint, processor), Idscp2MessageListener {
    override fun doStart() {
        super.doStart()
        endpoint.addConsumer(this)
    }

    override fun doStop() {
        endpoint.removeConsumer(this)
        super.doStop()
    }

    override fun onMessage(connection: Idscp2Connection, type: String, data: ByteArray) {
        val exchange = endpoint.createExchange()
        try {
            createUoW(exchange)
            // Set relevant information
            exchange.getIn().setHeader("idscp2.type", type)
            exchange.getIn().setBody(data, ByteArray::class.java)
            // Do processing
            processor.process(exchange)
            // Handle response
            val response = exchange.message
            val responseType = response.getHeader("idscp2.type", String::class.java)
            connection.send(responseType, response.getBody(ByteArray::class.java))
        } catch (e: Exception) {
            LOG.error("Error in Idscp2ServerConsumer.onMessage()", e)
        } finally {
            doneUoW(exchange)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientConsumer::class.java)
    }

}