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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.io.InputStream;
import java.io.Reader;

/**
 * A camel consumer retrieves an message, wraps in a Camel Exchange object and feeds it into Camel.
 *
 * <p>This is basically the implementation of a web socket server which accepts messages and sends
 * them to Camel.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public class WsConsumer extends DefaultConsumer {

  public WsConsumer(WsEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
  }

  @Override
  public void start() {
    super.start();
    getEndpoint().connect(this);
  }

  @Override
  public void stop() {
    getEndpoint().disconnect(this);
    super.stop();
  }

  @Override
  public WsEndpoint getEndpoint() {
    return (WsEndpoint) super.getEndpoint();
  }

  public void sendMessage(String message) {
    sendMessageInternal(message);
  }

  public void sendMessage(Throwable throwable) {
    sendMessageInternal(throwable);
  }

  public void sendMessage(byte[] message) {
    sendMessageInternal(message);
  }

  public void sendMessage(InputStream message) {
    sendMessageInternal(message);
  }

  public void sendMessage(Reader message) {
    sendMessageInternal(message);
  }

  private void sendMessageInternal(Object message) {
    final Exchange exchange = getEndpoint().createExchange();

    // TODO may set some headers with some meta info (e.g., socket info, unique-id for correlation
    // purpose, etc0
    // set the body

    if (message instanceof Throwable) {
      exchange.setException((Throwable) message);
    } else {
      exchange.getIn().setBody(message);
    }

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
