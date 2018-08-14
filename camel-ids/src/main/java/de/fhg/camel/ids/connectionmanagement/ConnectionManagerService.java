/*-
 * ========================LICENSE_START=================================
 * camel-ids
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.camel.ids.connectionmanagement;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.camel.ids.client.WsEndpoint;
import de.fhg.camel.ids.server.WebsocketComponent;
import de.fhg.camel.ids.server.WebsocketComponentServlet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point of the Connection Management Layer.
 *
 * <p>This class is exposed as an OSGi Service and serves to access connection data from the
 * management layer and REST API.
 *
 * @author Gerd Brost(gerd.brost@aisec.fraunhofer.de)
 */
@Component(enabled = true, immediate = true, name = "ids-conm")
public class ConnectionManagerService implements ConnectionManager {
  private static final Logger LOG = LoggerFactory.getLogger(ConnectionManagerService.class);

  @Activate
  protected void activate() {
    LOG.info("Activating Connection Manager");
  }

  @Deactivate
  protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
    LOG.info("Deactivating Connection Manager");
  }

  @Override
  public List<IDSCPServerEndpoint> listAvailableEndpoints() {
    return WebsocketComponent.getConnectors()
        .entrySet()
        .stream()
        .map(
            cEntry -> {
              IDSCPServerEndpoint endpoint = new IDSCPServerEndpoint();
              endpoint.setHost(cEntry.getValue().getConnector().getHost());
              endpoint.setPort(Integer.toString(cEntry.getValue().getConnector().getPort()));
              endpoint.setDefaultProtocol(cEntry.getValue().getConnector().getDefaultProtocol());
              endpoint.setEndpointIdentifier(cEntry.getKey());
              return endpoint;
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<IDSCPIncomingConnection> listIncomingConnections() {
    return WebsocketComponent.getConnectors()
        .values()
        .stream()
        .flatMap(
            connectorRef -> {
              WebsocketComponentServlet servlet = connectorRef.getServlet();
              // Servlet only present if an incoming connection exists. If null, do not collect
              // consumer information.
              if (servlet != null) {
                // Every connection has a websocket. We collect connection information this way.
                return connectorRef
                    .getMemoryStore()
                    .getAll()
                    .stream()
                    .map(
                        dws -> {
                          IDSCPIncomingConnection incomingConnection =
                              new IDSCPIncomingConnection();
                          incomingConnection.setEndpointIdentifier(
                              servlet.getConsumer().getEndpoint().toString());
                          incomingConnection.setEndpointKey(dws.getConnectionKey());
                          incomingConnection.setRemoteHostName(dws.getRemoteHostname());
                          incomingConnection.setAttestationResult(dws.getAttestationResult());
                          incomingConnection.setMetaData(dws.getMetaResult());
                          return incomingConnection;
                        });
              } else {
                return Stream.empty();
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<IDSCPOutgoingConnection> listOutgoingConnections() {
    return WsEndpoint.getOutgoingConnections();
  }
}
