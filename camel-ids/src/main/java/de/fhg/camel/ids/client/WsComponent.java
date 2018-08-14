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
package de.fhg.camel.ids.client;

import org.apache.camel.component.ahc.AhcComponent;
import org.apache.camel.component.ahc.AhcEndpoint;

import java.net.URI;

import static de.fhg.camel.ids.server.WebsocketConstants.WSS_PROTOCOL;
import static de.fhg.camel.ids.server.WebsocketConstants.WS_PROTOCOL;

public class WsComponent extends AhcComponent {

  @Override
  protected String createAddressUri(String uri, String remaining) {
    if (uri.startsWith("idsclientplain:")) {
      return uri.replaceFirst("idsclientplain", WS_PROTOCOL);
    } else if (uri.startsWith("idsclient:")) {
      return uri.replaceFirst("idsclient", WSS_PROTOCOL);
    }
    // Should not happen
    return uri;
  }

  @Override
  protected AhcEndpoint createAhcEndpoint(String endpointUri, AhcComponent component, URI httpUri) {
    return new WsEndpoint(endpointUri, (WsComponent) component);
  }
}
