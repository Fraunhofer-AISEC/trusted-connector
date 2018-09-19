/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.ids.comm.unixsocket;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixSocketThread implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(UnixSocketThread.class);
  private static final int SOCKET_FILE_RETRIES = 10;
  private static final long HALF_SEC_MS = 500;
  private final String socket;

  // The selector we'll be monitoring
  private final Selector selector;

  // The buffer into which we'll read data when it's available
  private ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);

  // The buffer into which we'll read data when it's available
  private byte[] readBufferLength = new byte[4];

  // A list of PendingChange instances
  private final List<ChangeRequest> pendingChanges = new LinkedList<>();

  // Maps a UnixSocketChannel to a list of ByteBuffer instances
  private final Map<UnixSocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();

  // Maps a UnixSocketChannel to a UnixSocketResponseHandler
  private Map<UnixSocketChannel, UnixSocketResponseHandler> rspHandlers =
      Collections.synchronizedMap(new HashMap<>());
  private boolean stopped = false;

  // default constructor
  public UnixSocketThread() throws IOException {
    this("SOCKET");
  }

  // constructor setting another socket address
  public UnixSocketThread(String socket) throws IOException {
    File s = new File(socket);
    if (!s.exists()) {
      throw new IOException("tpmd socket does not exist: " + s.getAbsolutePath());
    }
    this.socket = socket;
    this.selector = this.initSelector();
  }

  // send some data to the unix socket
  public void send(byte[] data, UnixSocketResponseHandler handler, boolean withLengthHeader)
      throws IOException, InterruptedException {
    byte[] result = data;
    // if message has to be sent with length header
    if (withLengthHeader) {
      // then append the length of the message
      int length = data.length;
      // in the first 4 bytes
      ByteBuffer bb = ByteBuffer.allocate(4 + length);
      bb.put(ByteBuffer.allocate(4).putInt(length).array());
      bb.put(data);
      result = bb.array();
    }
    // Start a new connection
    UnixSocketChannel channel = this.initiateConnection();
    // Register the response handler
    this.rspHandlers.put(channel, handler);
    // And queue the data we want written
    synchronized (this.pendingData) {
      List<ByteBuffer> queue = this.pendingData.computeIfAbsent(channel, ch -> new ArrayList<>());
      queue.add(ByteBuffer.wrap(result));
    }
    // Finally, wake up our selecting thread so it can make the required changes
    this.selector.wakeup();
  }

  // thread run method
  @Override
  public void run() {
    while (!stopped) {
      try {
        // Process any pending changes
        synchronized (this.pendingChanges) {
          for (ChangeRequest change : this.pendingChanges) {
            if (ChangeRequest.CHANGEOPS == change.type) {
              SelectionKey key = change.channel.keyFor(this.selector);
              key.interestOps(change.ops);
            } else if (ChangeRequest.REGISTER == change.type) {
              change.channel.register(this.selector, change.ops);
            }
          }
          this.pendingChanges.clear();
        }

        // Wait for an event on one of the registered channels
        int updates = this.selector.select();
        if (updates == 0) {  // Throttle
        	Thread.sleep(5);
        }
        LOG.trace("Reading from socket {}", this.socket);

        // Iterate over the set of keys for which events are available
        Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          LOG.trace("Read key: {}", key);
          selectedKeys.remove();
          if (!key.isValid()) {
            continue;
          }
          // Check what event is available and deal with it
          if (key.isConnectable()) {
            this.finishConnection(key);
          } else if (key.isReadable()) {
            this.read(key);
          } else if (key.isWritable()) {
            this.write(key);
          }
        }
      } catch (Exception e) {
        LOG.error("Error in UnixSocketThread", e);
      }
    }
  }

  // read send some data from the unix socket
  private void read(SelectionKey key) throws IOException {
    UnixSocketChannel channel = this.getChannel(key);

    // Clear out our read buffer so it's ready for new data
    this.readBuffer.clear();

    // Attempt to read off the channel
    int numRead;
    try {
      numRead = channel.read(this.readBuffer);
    } catch (IOException e) {
      // The remote forcibly closed the connection, cancel the selection key and close the channel.
      key.cancel();
      channel.close();
      return;
    }
    LOG.debug("bytes read: {}", numRead);
    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
      // channel.
      key.channel().close();
      key.cancel();
    } else if (numRead == 4) {
      // buffer length comes alone
      LOG.trace("Message header with length arrived!");
      System.arraycopy(
          Arrays.copyOfRange(this.readBuffer.array(), 0, 4), 0, this.readBufferLength, 0, 4);
    } else {
      // buffer length + protobuf message
      if (this.bufferLengthIsAppended(this.readBuffer, numRead)) {
        int length = new BigInteger(this.readBufferLength).intValue();
        LOG.trace("Message (with header) of length " + length + " arrived!");
        // Handle the read data
        this.handleResponse(channel, Arrays.copyOfRange(this.readBuffer.array(), 4, numRead));
      } else {
        int length = new BigInteger(this.readBufferLength).intValue();
        LOG.trace("Message (without header) of length " + length + " arrived!");
        // Handle the read data
        this.handleResponse(channel, this.readBuffer.array());
      }
    }
  }

  // check if the first 4 bytes of a ByteBuffer equals the length of the rest of the buffer
  private boolean bufferLengthIsAppended(ByteBuffer buf, int length) {
    if ((length - 4) == new BigInteger(Arrays.copyOfRange(buf.array(), 0, 4)).intValue()) {
      System.arraycopy(Arrays.copyOfRange(buf.array(), 0, 4), 0, this.readBufferLength, 0, 4);
      return true;
    } else {
      return false;
    }
  }

  // function to handle the data read from unix socket
  private void handleResponse(UnixSocketChannel channel, byte[] data) throws IOException {
    int length = new BigInteger(this.readBufferLength).intValue();

    // Make a correctly sized copy of the data before handing it to the client
    byte[] rspData = new byte[length];
    System.arraycopy(data, 0, rspData, 0, length);

    // Look up the handler for this channel
    UnixSocketResponseHandler handler = (UnixSocketResponseHandler) this.rspHandlers.get(channel);
    LOG.trace("Unixsocketthread received: " + rspData.length);

    // And pass the response to it
    if (handler.handleResponse(rspData)) {
      // The handler has seen enough, close the connection
      channel.close();
      channel.keyFor(this.selector).cancel();
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
        // We wrote away all data, so we're no longer interested in writing on this socket. Switch
        // back to waiting for data.
        key.interestOps(SelectionKey.OP_READ);
      }
    }
  }

  private void finishConnection(SelectionKey key) throws IOException {
    final UnixSocketChannel channel = this.getChannel(key);

    // Finish the connection. If the connection operation failed this will raise an IOException.
    try {
      channel.finishConnect();
    } catch (IOException e) {
      // Cancel the channel's registration with our selector
      LOG.debug(e.getMessage(), e);
      key.cancel();
      throw e;
    }

    // Register an interest in writing on this channel
    key.interestOps(SelectionKey.OP_WRITE);
  }

  private UnixSocketChannel initiateConnection() throws IOException, InterruptedException {
    // open the socket address
    File socketFile = new File(socket);
    // Try to open socket file 10 times
    int retries = 0;
    while (!socketFile.getAbsoluteFile().exists() && retries < SOCKET_FILE_RETRIES) {
      ++retries;
      TimeUnit.MILLISECONDS.sleep(HALF_SEC_MS);
      socketFile = new File(socket);
      if (retries < SOCKET_FILE_RETRIES && LOG.isDebugEnabled()) {
        LOG.debug(
            String.format(
                "error: socket \"%s\" does not exist after %s retry.",
                socketFile.getAbsolutePath(), retries));
      }
    }
    if (!socketFile.getAbsoluteFile().exists()) {
      throw new IOException(
          "Could not connect to Unix socket after 10 retries: " + socketFile.getAbsoluteFile());
    }
    UnixSocketAddress address = new UnixSocketAddress(socketFile.getAbsoluteFile());
    UnixSocketChannel channel = UnixSocketChannel.open(address);
    channel.configureBlocking(false);
    // synchronize pending changes
    synchronized (this.pendingChanges) {
      this.pendingChanges.add(
          new ChangeRequest(channel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
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

  public void terminate() {
    this.stopped = true;
    Thread.currentThread().interrupt();
  }
}
