/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.aisec.ids.comm.unixsocket;

import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TrustmeUnixSocketThread implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(TrustmeUnixSocketThread.class);
  private String socket;

  // The selector we'll be monitoring
  private final Selector selector;

  private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);

  // A list of PendingChange instances
  private final List<ChangeRequest> pendingChanges = new LinkedList<>();

  // Maps a UnixSocketChannel to a list of ByteBuffer instances
  private final Map<UnixSocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();

  // Maps a UnixSocketChannel to a UnixSocketResponseHandler
  private final Map<UnixSocketChannel, TrustmeUnixSocketResponseHandler> rspHandlers =
      Collections.synchronizedMap(new HashMap<>());

  // constructor setting another socket address
  public TrustmeUnixSocketThread(String socket) throws IOException {
    this.setSocket(socket);
    this.selector = this.initSelector();
  }

  // send a protobuf message to the unix socket
  public void sendWithHeader(byte[] data, TrustmeUnixSocketResponseHandler handler)
      throws IOException, InterruptedException {
    this.send(data, handler, true);
  }

  // send some data to the unix socket
  public void send(byte[] data, TrustmeUnixSocketResponseHandler handler, boolean withLengthHeader)
      throws IOException, InterruptedException {
    LOG.debug("writing protobuf to socket");
    byte[] result = data;
    // if message has to be sent with length header
    if (withLengthHeader) {
      // then append the length of the message
      int length = data.length;
      // in the first 4 bytes
      lengthBuffer.clear();
      ByteBuffer bb = ByteBuffer.allocate(4 + length);
      bb.put(lengthBuffer.putInt(length).array());
      bb.put(data);
      result = bb.array();
    }
    UnixSocketChannel socket = initiateConnection();
    // Register the response handler
    this.rspHandlers.put(socket, handler);
    // And queue the data we want written
    synchronized (this.pendingData) {
      List<ByteBuffer> queue = this.pendingData.computeIfAbsent(socket, k -> new ArrayList<>());
      queue.add(ByteBuffer.wrap(result));
    }

    // Finally, wake up our selecting thread so it can make the required changes
    this.selector.wakeup();
  }

  // thread run method
  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        // Process any pending changes
        synchronized (this.pendingChanges) {
          for (ChangeRequest change : this.pendingChanges) {
            switch (change.type) {
              case ChangeRequest.CHANGEOPS:
                SelectionKey key = change.channel.keyFor(this.selector);
                key.interestOps(change.ops);
                break;
              case ChangeRequest.REGISTER:
                change.channel.register(this.selector, change.ops);
                break;
              default:
                LOG.warn("Unknown ChangeRequest type {}", change.type);
            }
          }
          this.pendingChanges.clear();
        }

        // Wait for an event on one of the registered channels
        this.selector.select();

        // Iterate over the set of keys for which events are available
        Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();
          if (!key.isValid()) {
            continue;
          }
          // Check what event is available and deal with it
          if (key.isConnectable()) {
            this.finishConnection(key);
          }
          if (key.isReadable()) {
            this.read(key);
          } else if (key.isWritable()) {
            this.write(key);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // read send some data from the unix socket
  private void read(SelectionKey key) throws IOException {
    LOG.debug("reading protobuf from socket");
    UnixSocketChannel channel = this.getChannel(key);

    int length = readMessageLength(key, channel);

    if (length == -1) {
      // Remote entity shut the socket down cleanly. Do the same from our end and
      // cancel the channel.
      LOG.debug("Closing channel because length = -1");
      key.channel().close();
      key.cancel();
      return;
    }

    ByteBuffer messageBuffer = ByteBuffer.allocate(length);

    messageBuffer.clear();

    int numRead = 0;
    int totalRead = 0;
    try {
      while (totalRead < length) {
        numRead = channel.read(messageBuffer);
        totalRead += numRead;
        LOG.debug("read {} bytes of protobuf from socket, {} in total", numRead, totalRead);
      }
    } catch (IOException e) {
      // The remote forcibly closed the connection, cancel the selection key and close
      // the channel.
      LOG.debug("error while reading from socket", e);
      key.cancel();
      channel.close();
      return;
    }
    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
      // channel.
      LOG.debug("Closing channel because numRead = -1");
      key.channel().close();
      key.cancel();
      return;
    }

    // Handle the response
    this.handleResponse(channel, messageBuffer.array());
  }

  private int readMessageLength(SelectionKey key, UnixSocketChannel channel) throws IOException {
    // Clear out our read buffer so it's ready for new data
    lengthBuffer.clear();

    // Attempt to read off the channel
    int length;
    try {
      int numread = channel.read(lengthBuffer);
      if (numread == 4) {
        length = new BigInteger(lengthBuffer.array()).intValue();
      } else {
        length = -1;
      }
    } catch (IOException e) {
      // The remote forcibly closed the connection, cancel the selection key and close
      // the channel.
      LOG.debug("error while reading message length from socket", e);
      key.cancel();
      channel.close();
      return -1;
    }
    LOG.debug("read message from UNIX socket with length " + length);
    return length;
  }

  private void handleResponse(UnixSocketChannel socketChannel, byte[] data) throws IOException {
    // Make a correctly sized copy of the data before handing it
    // to the client
    byte[] rspData = new byte[data.length];
    System.arraycopy(data, 0, rspData, 0, data.length);

    // Look up the handler for this channel
    TrustmeUnixSocketResponseHandler handler = this.rspHandlers.get(socketChannel);

    // And pass the response to it
    if (handler.handleResponse(rspData)) {
      LOG.debug("Handler done, close channel");
      // The handler has seen enough, close the connection
      socketChannel.close();
      socketChannel.keyFor(this.selector).cancel();
    }
  }

  private void write(SelectionKey key) throws IOException {
    final UnixSocketChannel channel = this.getChannel(key);

    synchronized (this.pendingData) {
      List<ByteBuffer> queue = this.pendingData.get(channel);

      // Write until there's not more data
      while (!queue.isEmpty()) {
        ByteBuffer buf = queue.get(0);
        channel.write(buf);
        if (buf.remaining() > 0) {
          // ... or the socket's buffer fills up
          break;
        }
        queue.remove(0);
      }
      if (queue.isEmpty()) {
        // We wrote away all data, so we're no longer interested in writing on this
        // socket. Switch back to waiting for data.
        key.interestOps(SelectionKey.OP_READ);
      }
    }
  }

  private void finishConnection(SelectionKey key) {
    final UnixSocketChannel channel = this.getChannel(key);

    // Finish the connection. If the connection operation failed this will raise an
    // IOException.
    try {
      System.out.println(channel.finishConnect());
    } catch (IOException e) {
      // Cancel the channel's registration with our selector
      LOG.debug(e.getMessage(), e);
      key.cancel();
      return;
    }

    // Register an interest in writing on this channel
    // this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS,
    // SelectionKey.OP_WRITE));
    // key.interestOps(SelectionKey.OP_WRITE);
    this.selector.wakeup();
  }

  private UnixSocketChannel initiateConnection() throws IOException, InterruptedException {
    // open the socket address
    File socketFile = new File(getSocket());
    // Try to open socket file 10 times
    int retries = 0;
    while (!socketFile.getAbsoluteFile().exists() && retries < 10) {
      ++retries;
      TimeUnit.MILLISECONDS.sleep(500L);
      socketFile = new File(getSocket());
      if (retries < 10) {
        LOG.debug(
            String.format(
                "error: socket \"%s\" does not exist after %s retry.",
                socketFile.getAbsolutePath(), retries));
      }
    }
    UnixSocketAddress address = new UnixSocketAddress(socketFile.getAbsoluteFile());
    UnixSocketChannel channel = UnixSocketChannel.open(address);
    channel.configureBlocking(false);
    // synchronize pending changes
    synchronized (this.pendingChanges) {
      this.pendingChanges.add(
          new ChangeRequest(
                  channel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE));
    }
    return channel;
  }

  // initialize the selector
  private Selector initSelector() throws IOException {
    return NativeSelectorProvider.getInstance().openSelector();
  }

  // get the channel
  private UnixSocketChannel getChannel(SelectionKey k) {
    return (UnixSocketChannel) k.channel();
  }

  public String getSocket() {
    return socket;
  }

  public void setSocket(String socket) {
    this.socket = socket;
  }
}
