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

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import org.apache.camel.Exchange
import org.apache.camel.support.DefaultProducer

/**
 * The IDSCP2 server producer.
 * Sends each message to all clients connected to this server endpoint.
 */
class Idscp2ServerProducer(private val endpoint: Idscp2ServerEndpoint) : DefaultProducer(endpoint) {

    override fun process(exchange: Exchange) {
        exchange.message.let {
            val type = it.getHeader(IDSCP2_HEADER)
            val body = it.getBody(ByteArray::class.java)
            if (type != null || body != null) {
                endpoint.sendMessage(type, body)
            }
        }
    }
}