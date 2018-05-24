/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
/**
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
package de.fhg.camel.ids.both;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.jetty.server.Server;
import org.junit.Ignore;
import org.junit.Test;

import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.camel.ids.IdsProtocolComponent;
import de.fhg.camel.ids.connectionmanagement.ConnectionManagerService;

public class WsProducerConsumerTest extends CamelTestSupport {
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final int PORT = AvailablePortFinder.getNextAvailable();
    protected Server server;
   
    protected List<Object> messages;
	private ConnectionManagerService conm;
	private IdsProtocolComponent idspc;
    
    @Override
    public void setUp() throws Exception {
    	idspc = new IdsProtocolComponent();
    	conm = new ConnectionManagerService();
		idspc.bindConnectionManager(conm);
        assertTrue(conm.listIncomingConnections().isEmpty());
    	super.setUp();
    	
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testTwoRoutes() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);
        
        template.sendBody("direct:input", TEST_MESSAGE);
        
        List<IDSCPIncomingConnection> incomings = conm.listIncomingConnections();
        //assertEquals(1,incomings.size());
        //TODO: Fix this test when connectionmanager is done
        //AttestationResult ratResult = incomings.get(0).getAttestationResult();
        //assertEquals(AttestationResult.SKIPPED, ratResult);
        
        mock.assertIsSatisfied();
    }

    @Test
    @Ignore
    public void testTwoRoutesRestartConsumer() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();

        resetMocks();

        log.info("Restarting bar route");
        context.stopRoute("bar");
        Thread.sleep(500);
        context.startRoute("bar");

        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();
    }

    @Test
    @Ignore
    public void testTwoRoutesRestartProducer() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();

        resetMocks();

        log.info("Restarting foo route");
        context.stopRoute("foo");
        Thread.sleep(500);
        context.startRoute("foo");

        mock.expectedBodiesReceived(TEST_MESSAGE);

        template.sendBody("direct:input", TEST_MESSAGE);

        mock.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder[] createRouteBuilders() throws Exception {
        RouteBuilder[] rbs = new RouteBuilder[2];
        rbs[0] = new RouteBuilder() {
            public void configure() {
                from("direct:input").routeId("foo")
                    .to("idsclientplain://localhost:" + PORT+"/?attestation=3");
            }
        };
        rbs[1] = new RouteBuilder() {
            public void configure() {
                from("idsserver://0.0.0.0:" + PORT+"/?attestation=3").routeId("bar")
                    .to("mock:result");
            }
        };
        return rbs;
    }
}