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

import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("idscp2server")
public class Idscp2ServerComponent extends DefaultComponent {
    final Map<Integer, Idscp2Server> servers = new ConcurrentHashMap<>();

    public Idscp2ServerComponent() {
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new Idscp2ServerEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    public synchronized void addServer(int port, Idscp2Server server) {
        servers.put(port, server);
    }

    @Override
    protected synchronized void doStop() throws Exception {
        for (Idscp2Server s : servers.values()) {
            s.terminate();
        }
        super.doStop();
    }
}
