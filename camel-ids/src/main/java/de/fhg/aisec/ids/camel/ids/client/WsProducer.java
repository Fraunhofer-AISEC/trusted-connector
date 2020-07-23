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
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;
import org.asynchttpclient.ws.WebSocket;

import java.io.IOException;
import java.io.InputStream;


public class WsProducer extends DefaultProducer {
  public static final int STREAM_BUFFER_SIZE = 8192;

  public WsProducer(WsEndpoint endpoint) {
    super(endpoint);
  }

  @Override
  public WsEndpoint getEndpoint() {
    return (WsEndpoint) super.getEndpoint();
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Message in = exchange.getIn();
    Object message = in.getBody();
    if (message != null) {
      if (log.isTraceEnabled()) {
        log.debug("Sending out {}", message);
      }
      if (message instanceof String) {
        sendMessage(getWebSocket(), (String) message, getEndpoint().isUseStreaming());
      } else if (message instanceof byte[]) {
        sendMessage(getWebSocket(), (byte[]) message, getEndpoint().isUseStreaming());
      } else if (message instanceof InputStream) {
        sendStreamMessage(getWebSocket(), (InputStream) message);
      } else {
        // TODO provide other binding option, for now use the converted string
        getWebSocket().sendTextFrame(in.getMandatoryBody(String.class));
      }
    }
  }

  public static void sendMessage(WebSocket webSocket, String msg, boolean streaming) {
    if (streaming && msg.length() > STREAM_BUFFER_SIZE) {
      int p = 0;
      while (p < msg.length()) {
        if (msg.length() - p < STREAM_BUFFER_SIZE) {
          webSocket.sendContinuationFrame(msg.substring(p), true, 0);
          p = msg.length();
        } else if (p == 0) {
          p = STREAM_BUFFER_SIZE;
          webSocket.sendTextFrame(msg.substring(0, STREAM_BUFFER_SIZE), false, 0);
        } else {
          var substring = msg.substring(p, p + STREAM_BUFFER_SIZE);
          p += STREAM_BUFFER_SIZE;
          webSocket.sendContinuationFrame(substring, msg.length() == p, 0);
        }
      }
    } else {
      webSocket.sendTextFrame(msg);
    }
  }

  public static void sendMessage(WebSocket webSocket, byte[] msg, boolean streaming) {
    if (streaming && msg.length > STREAM_BUFFER_SIZE) {
      int p = 0;
      byte[] writebuf = new byte[STREAM_BUFFER_SIZE];
      while (p < msg.length) {
        if (msg.length - p < STREAM_BUFFER_SIZE) {
          int rest = msg.length - p;
          // bug in grizzly? we need to create a byte array with the exact length
          byte[] tmpbuf = new byte[rest];
          System.arraycopy(msg, p, tmpbuf, 0, rest);
          p = msg.length;
          webSocket.sendContinuationFrame(tmpbuf, true, 0);
        } else if (p == 0) {
          System.arraycopy(msg, p, writebuf, 0, STREAM_BUFFER_SIZE);
          p = STREAM_BUFFER_SIZE;
          webSocket.sendBinaryFrame(writebuf, false, 0);
        } else {
          System.arraycopy(msg, p, writebuf, 0, STREAM_BUFFER_SIZE);
          p += STREAM_BUFFER_SIZE;
          webSocket.sendContinuationFrame(writebuf, msg.length == p, 0);
        }
      }
    } else {
      webSocket.sendBinaryFrame(msg);
    }
  }

  public static void sendStreamMessage(WebSocket webSocket, InputStream in) throws IOException {
    try (in) {
      byte[] buffer = new byte[STREAM_BUFFER_SIZE];
      int rn;
      if ((rn = in.read(buffer, 0, STREAM_BUFFER_SIZE)) < STREAM_BUFFER_SIZE) {
        if (rn >= 0) {
          byte[] smallBuffer = new byte[rn];
          if (rn > 0) {
            System.arraycopy(buffer, 0, smallBuffer, 0, rn);
          }
          webSocket.sendBinaryFrame(smallBuffer, true, 0);
        }
      } else {
        byte[] nextBuffer = new byte[STREAM_BUFFER_SIZE];
        int rnNext = in.read(nextBuffer, 0, STREAM_BUFFER_SIZE);
        // Send first (maybe last) frame
        webSocket.sendBinaryFrame(buffer, rnNext <= 0, 0);
        while (rnNext == STREAM_BUFFER_SIZE) {
          // swap buffer and nextBuffer
          byte[] swap = buffer;
          buffer = nextBuffer;
          nextBuffer = swap;
          rnNext = in.read(nextBuffer, 0, STREAM_BUFFER_SIZE);
          // Send chunk read in previous round
          webSocket.sendContinuationFrame(buffer, rnNext <= 0, 0);
        }
        // Send final chunk with length-adjusted byte array
        if (rnNext > 0) {
          byte[] finalBuffer = new byte[rnNext];
          System.arraycopy(nextBuffer, 0, finalBuffer, 0, rnNext);
          webSocket.sendContinuationFrame(finalBuffer, true, 0);
        }
      }
    }
  }

  private WebSocket getWebSocket() {
    return getEndpoint().getWebSocket();
  }
}
