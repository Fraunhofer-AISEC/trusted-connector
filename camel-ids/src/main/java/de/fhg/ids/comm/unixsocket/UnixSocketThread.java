package de.fhg.ids.comm.unixsocket;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class UnixSocketThread implements Runnable {
	private Logger LOG = LoggerFactory.getLogger(UnixSocketThread.class);
	private UnixSocketAddress address;
	private UnixSocketChannel channel;
	private String SOCKET = "/tmp/tpm2d.sock";

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();

	// Maps a UnixSocketChannel to a list of ByteBuffer instances
	private Map<UnixSocketChannel, List<ChangeRequest>> pendingData = new HashMap<UnixSocketChannel, List<ChangeRequest>>();
	
	// Maps a UnixSocketChannel to a UnixSocketResponseHandler
	private Map<UnixSocketChannel, UnixSocketResponsHandler> rspHandlers = Collections.synchronizedMap(new HashMap<UnixSocketChannel, UnixSocketResponsHandler>());
	
	public UnixSocketThread() throws IOException {
		this.selector = this.initSelector();
	}

	public UnixSocketThread(String socket) throws IOException {
		this.SOCKET = socket;
		this.selector = this.initSelector();
	}

	public void send(byte[] data, UnixSocketResponsHandler handler) throws IOException {
		// Start a new connection
		UnixSocketChannel channel = this.initiateConnection();
		
		// Register the response handler
		this.rspHandlers.put(channel, handler);
		
		// And queue the data we want written
		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(channel);
			if (queue == null) {
				queue = new ArrayList();
				this.pendingData.put(channel, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		// Finally, wake up our selecting thread so it can make the required changes
		this.selector.wakeup();
	}

	public void run() {
		while (true) {
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
						case ChangeRequest.REGISTER:
							change.channel.register(this.selector, change.ops);
							break;
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
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
					if (key.isConnectable()) {
						this.finishConnection(key);
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

	private void read(SelectionKey key) throws IOException {
		UnixSocketChannel channel = this.getChannel(key);

		// Clear out our read buffer so it's ready for new data
		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = channel.read(this.readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			channel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		// Handle the response
		this.handleResponse(channel, this.readBuffer.array(), numRead);
	}

	private void handleResponse(UnixSocketChannel channel, byte[] data, int numRead) throws IOException {
		// Make a correctly sized copy of the data before handing it
		// to the client
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);
		
		// Look up the handler for this channel
		UnixSocketResponsHandler handler = (UnixSocketResponsHandler) this.rspHandlers.get(channel);
		
		// And pass the response to it
		if (handler.handleResponse(rspData)) {
			// The handler has seen enough, close the connection
			channel.close();
			channel.keyFor(this.selector).cancel();
		}
	}

	private void write(SelectionKey key) throws IOException {
		UnixSocketChannel channel = this.getChannel(key);

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(channel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				channel.write(buf);
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		UnixSocketChannel channel = this.getChannel(key);
	
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			channel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
	
		// Register an interest in writing on this channel
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private UnixSocketChannel initiateConnection() throws IOException {
		// open socket file
		File socketFile = new File(SOCKET);
		LOG.debug("opening socket: "+ SOCKET);
		// Try to open socket file 10 times
		int retries = 0;
        while (!socketFile.getAbsoluteFile().exists()) {
            try {
            	++retries;
            	TimeUnit.MILLISECONDS.sleep(250L);
				socketFile = new File(SOCKET);
			} catch (Exception e) {
				LOG.info("Bad news everyone: " + e.getMessage());
				e.printStackTrace();
			}
            if (retries < 10) {
            	LOG.info(String.format("socket %s does not exist after %s retry.", socketFile.getAbsolutePath(), retries));
            	return null;
            }
        }
		this.address = new UnixSocketAddress(socketFile.getAbsoluteFile());	
		this.channel = UnixSocketChannel.open(this.address);
		this.channel.configureBlocking(false);
		synchronized(this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		}
		
		return this.channel;
	}


	private Selector initSelector() throws IOException {
		return NativeSelectorProvider.getInstance().openSelector();
	}
	
	private UnixSocketChannel getChannel(SelectionKey k) {
		return (UnixSocketChannel) k.channel();
	}

}
