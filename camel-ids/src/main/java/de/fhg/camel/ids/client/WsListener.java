package de.fhg.camel.ids.client;

import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class WsListener implements WebSocketListener {
  private static final Logger LOG = LoggerFactory.getLogger(WsListener.class);

  private final Set<WsConsumer> consumers;
  private final WsEndpoint endpoint;

  public WsListener(Set<WsConsumer> consumers, WsEndpoint endpoint) {
    this.consumers = consumers;
    this.endpoint = endpoint;
  }

  @Override
  public void onOpen(WebSocket websocket) {
    LOG.debug("Websocket opened");
  }

  @Override
  public void onClose(WebSocket websocket, int code, String status) {
    LOG.debug("websocket closed - reconnecting");
    try {
      this.endpoint.reConnect();
    } catch (Exception e) {
      LOG.warn("Error re-connecting to websocket", e);
    }
  }

  @Override
  public void onError(Throwable t) {
    LOG.debug("websocket on error", t);
    if (endpoint.isSendMessageOnError()) {
      for (WsConsumer consumer : consumers) {
        consumer.sendMessage(t);
      }
    }
  }

  @Override
  public void onBinaryFrame(byte[] message, boolean finalFragment, int rsv) {
    LOG.debug("Received message --> {}", message);
    for (WsConsumer consumer : consumers) {
      consumer.sendMessage(message);
    }
  }

  @Override
  public void onTextFrame(String message, boolean finalFragment, int rsv) {
    LOG.debug("Received message --> {}", message);
    for (WsConsumer consumer : consumers) {
      consumer.sendMessage(message);
    }
  }
}