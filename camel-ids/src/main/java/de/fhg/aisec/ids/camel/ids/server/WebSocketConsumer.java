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
package de.fhg.aisec.ids.camel.ids.server;

import de.fhg.aisec.ids.camel.ids.WebSocketConstants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class WebSocketConsumer extends DefaultConsumer implements WebSocketProducerConsumer {

  private final WebSocketEndpoint endpoint;

  public WebSocketConsumer(WebSocketEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
    this.endpoint = endpoint;
  }

  @Override
  public void doStart() throws Exception {
    super.doStart();
    endpoint.connect(this);
  }

  @Override
  public void doStop() throws Exception {
    endpoint.disconnect(this);
    super.doStop();
  }

  public WebSocketEndpoint getEndpoint() {
    return endpoint;
  }

  public int getAttestationType() {
    return endpoint.getAttestation();
  }

  public int getAttestationMask() {
    return endpoint.getAttestationMask();
  }

  public void sendMessage(final String connectionKey, final String message) {
    sendMessage(connectionKey, (Object) message);
  }

  public void sendMessage(final String connectionKey, final Object message) {

    final Exchange exchange = getEndpoint().createExchange();

    // set header and body
    exchange.getIn().setHeader(WebSocketConstants.CONNECTION_KEY, connectionKey);
    exchange.getIn().setBody(message);

    // send exchange using the async routing engine
    getAsyncProcessor()
        .process(
            exchange,
            doneSync -> {
              if (exchange.getException() != null) {
                getExceptionHandler()
                    .handleException(
                        "Error processing exchange", exchange, exchange.getException());
              }
            });
  }
}
