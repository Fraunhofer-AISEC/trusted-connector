/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.ids.cm.impl.trustx;

import de.fhg.ids.comm.unixsocket.ChangeRequest;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustXMock implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(TrustXMock.class);
  private UnixSocketAddress address;
  private UnixServerSocketChannel channel;
  private String socket;

  // The selector we'll be monitoring
  private Selector selector;

  // The buffer into which we'll read data when it's available
  private ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024);

  // A list of PendingChange instances
  private List<ChangeRequest> pendingChanges = new LinkedList<>();

  // Maps a UnixSocketChannel to a list of ByteBuffer instances
  private Map<UnixSocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();

  private TrustXMockHandler handler;

  // constructor setting another socket address
  public TrustXMock(String socket, TrustXMockHandler handler) throws IOException {
    this.setSocket(socket);
    this.selector = this.initSelector();
    this.handler = handler;
  }

  // send a protobuf message to the unix socket
  public void send(UnixSocketChannel socket, byte[] data) {
    synchronized (this.pendingChanges) {
      this.pendingChanges.add(
          new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
      // And queue the data we want written
      synchronized (this.pendingData) {
        List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(socket);
        if (queue == null) {
          queue = new ArrayList<ByteBuffer>();
          this.pendingData.put(socket, queue);
        }
        queue.add(ByteBuffer.wrap(data));
      }
      // Finally, wake up our selecting thread so it can make the required changes
      this.selector.wakeup();
    }
  }

  // thread run method
  @Override
  public void run() {
    while (!Thread.interrupted()) {
      try {
        // Process any pending changes
        synchronized (this.pendingChanges) {
          Iterator<ChangeRequest> changes = this.pendingChanges.iterator();
          while (changes.hasNext()) {
            ChangeRequest change = (ChangeRequest) changes.next();
            switch (change.type) {
              case ChangeRequest.CHANGEOPS:
                SelectionKey key = change.channel.keyFor(this.selector);
                key.interestOps(change.ops);
                break;
            }
          }
          this.pendingChanges.clear();
        }

        // Wait for an event on one of the registered channels
        this.selector.select();

        // Iterate over the set of keys for which events are available
        Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
          SelectionKey key = (SelectionKey) selectedKeys.next();
          selectedKeys.remove();
          if (!key.isValid()) {
            continue;
          }
          // Check what event is available and deal with it
          if (key.isAcceptable()) {
            this.accept(key);
          } else if (key.isReadable()) {
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

  private void accept(SelectionKey key) throws IOException {
    // Accept the connection and make it non-blocking
    UnixSocketChannel client = channel.accept();
    client.configureBlocking(false);

    // Register the new SocketChannel with our Selector, indicating
    // we'd like to be notified when there's data waiting to be read
    client.register(this.selector, SelectionKey.OP_READ);
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
    LOG.debug("bytes read: " + numRead);
    if (numRead == -1) {
      // Remote entity shut the socket down cleanly. Do the same from our end and cancel the
      // channel.
      key.channel().close();
      key.cancel();
      return;
    }
    handler.handleResponse(this, channel, this.readBuffer.array(), numRead);
  }

  private void write(SelectionKey key) throws IOException {
    final UnixSocketChannel channel = this.getChannel(key);

    synchronized (this.pendingData) {
      List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(channel);

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

  // initialize the selector
  private Selector initSelector() throws IOException {
    Selector socketSelector = NativeSelectorProvider.getInstance().openSelector();

    File socketFile = new File(getSocket());
    socketFile.delete();
    socketFile.deleteOnExit();

    this.address = new UnixSocketAddress(socketFile.getAbsoluteFile());
    this.channel = UnixServerSocketChannel.open();
    this.channel.configureBlocking(false);
    this.channel.socket().bind(address);

    channel.register(socketSelector, SelectionKey.OP_ACCEPT);

    return socketSelector;
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

  public static void main(String[] args) {
    try {
      TrustXMockHandler handler = new TrustXMockHandler();
      new Thread(handler).start();
      new Thread(new TrustXMock("src/test/socket/trustme.sock", handler)).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
