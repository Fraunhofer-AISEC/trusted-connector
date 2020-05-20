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
package de.fhg.aisec.ids.camel.idscp2.server;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageListener;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The IDSCP2 server consumer.
 */
public class Idscp2ServerConsumer extends DefaultConsumer implements Idscp2MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ServerConsumer.class);
    private final Idscp2ServerEndpoint endpoint;

    public Idscp2ServerConsumer(final Idscp2ServerEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        endpoint.addConsumer(this);
    }

    @Override
    protected void doStop() throws Exception {
        endpoint.removeConsumer(this);
        super.doStop();
    }

    @Override
    public void onMessage(Idscp2Connection connection, String type, byte[] data) {
        final var exchange = endpoint.createExchange();
        try {
            this.createUoW(exchange);
            // Set relevant information
            exchange.getIn().setHeader("idscp2.type", type);
            exchange.getIn().setBody(data, byte[].class);
            // Do processing
            getProcessor().process(exchange);
            // Handle response
            Message response = exchange.getMessage();
            String responseType = response.getHeader("idscp2.type", String.class);
            connection.send(responseType, response.getBody(byte[].class));
        } catch (Exception e) {
            LOG.error("Error in Idscp2ServerConsumer.onMessage()", e);
        } finally {
            this.doneUoW(exchange);
        }
    }
}
