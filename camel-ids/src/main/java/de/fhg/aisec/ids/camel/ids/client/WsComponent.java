/*-
 * ========================LICENSE_START=================================
 * camel-ids
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.camel.ids.client;

import de.fhg.aisec.ids.camel.ids.IdscpConstants;
import de.fhg.aisec.ids.camel.ids.WebSocketConstants;
import org.apache.camel.component.ahc.AhcComponent;
import org.apache.camel.component.ahc.AhcEndpoint;

import java.net.URI;

public class WsComponent extends AhcComponent {

  @Override
  protected String createAddressUri(String uri, String remaining) {
    if (uri.startsWith(IdscpConstants.WSS_PROTOCOL + ":")) {
      uri = uri.replaceFirst(IdscpConstants.WSS_PROTOCOL, WebSocketConstants.WSS_PROTOCOL);
    } else if (uri.startsWith(IdscpConstants.WS_PROTOCOL + ":")) {
      uri = uri.replaceFirst(IdscpConstants.WS_PROTOCOL, WebSocketConstants.WS_PROTOCOL);
    }
    // Should not happen
    return uri;
  }

  @Override
  protected AhcEndpoint createAhcEndpoint(String endpointUri, AhcComponent component, URI httpUri) {
    return new WsEndpoint(endpointUri, (WsComponent) component);
  }
}
