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

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.camel.ids.IdsProtocolComponent;
import de.fhg.camel.ids.connectionmanagement.ConnectionManagerService;

public class IdsClientServerPlaintextWithAttestationTest extends CamelTestSupport {
    protected static final String TEST_MESSAGE = "Hello World!";
    protected static final String TEST_MESSAGE_2 = "Hello Again!";
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
    public void testFromRouteAToB() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        
        // Send a test message into begin of client route
        template.sendBody("direct:input", TEST_MESSAGE);
        
        // We expect that mock endpoint is happy and has received a message
        mock.assertIsSatisfied();
        mock.expectedBodiesReceived(TEST_MESSAGE);
        
        // We expect one incoming connection to be listed by ConnectionManager
        List<IDSCPIncomingConnection> incomings = conm.listIncomingConnections();
        assertEquals(1,incomings.size());

        // We expect attestation to FAIL because unit tests have no valid TPM
        AttestationResult ratResult = incomings.get(0).getAttestationResult();
        assertEquals(AttestationResult.FAILED, ratResult);
        
        mock.assertIsSatisfied();
    }

    @Test
    public void testTwoRoutesRestartConsumer() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.reset();

        // Send a message
        template.sendBody("direct:input", TEST_MESSAGE);
        mock.expectedBodiesReceived(TEST_MESSAGE);

        // We expect that the message went through
        mock.assertIsSatisfied();

        // Clean the mocks
        resetMocks();

        // Now stop and start the client route
        log.info("Restarting client route");
        context.stopRoute("client");
        Thread.sleep(500);
        context.startRoute("client");

        template.sendBody("direct:input", TEST_MESSAGE_2);
        mock.expectedBodiesReceived(TEST_MESSAGE_2);
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
                from("direct:input").routeId("client")
                    .to("idsclientplain://localhost:9292/zero");
            }
        };
        rbs[1] = new RouteBuilder() {
            public void configure() {
                from("idsserver://0.0.0.0:9292/zero").routeId("server")
                    .to("mock:result");
            }
        };
        return rbs;
    }
}