package de.fhg.ids.comm.unixsocket;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class TrustmeUnixSocketThread implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(TrustmeUnixSocketThread.class);
	private UnixSocketAddress address;
	private UnixSocketChannel channel;
	private String socket;
	
	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024*1024);

	// A list of PendingChange instances
	private List<ChangeRequest> pendingChanges = new LinkedList<>();

	// Maps a UnixSocketChannel to a list of ByteBuffer instances
	private Map<UnixSocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();
	
	// Maps a UnixSocketChannel to a UnixSocketResponseHandler
	private Map<UnixSocketChannel, TrustmeUnixSocketResponseHandler> rspHandlers = Collections.synchronizedMap(new HashMap<UnixSocketChannel, TrustmeUnixSocketResponseHandler>());

	// constructor setting another socket address
	public TrustmeUnixSocketThread(String socket) throws IOException {
		this.setSocket(socket);
		this.selector = this.initSelector();
	}
	
	// send a protobuf message to the unix socket
	public void send(byte[] data, TrustmeUnixSocketResponseHandler handler) throws IOException, InterruptedException{
		
		UnixSocketChannel socket = initiateConnection();		
		// Register the response handler
		this.rspHandlers.put(socket, handler);
		// And queue the data we want written
		synchronized (this.pendingData) {
			List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList<ByteBuffer>();
				this.pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
			//System.out.println(new String(pendingData.get(socket).get(0).array()));
		}
		
		// Finally, wake up our selecting thread so it can make the required changes
		this.selector.wakeup();
	}
	
	// thread run method
	@Override
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
			// Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		
		// Handle the response
		this.handleResponse(channel, this.readBuffer.array(), numRead);
	}

	private void handleResponse(UnixSocketChannel socketChannel, byte[] data, int numRead) throws IOException {
		// Make a correctly sized copy of the data before handing it
		// to the client
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);
		
		// Look up the handler for this channel
		TrustmeUnixSocketResponseHandler handler = (TrustmeUnixSocketResponseHandler) this.rspHandlers.get(socketChannel);
		
		// And pass the response to it
		if (handler.handleResponse(rspData)) {
			// The handler has seen enough, close the connection
			socketChannel.close();
			socketChannel.keyFor(this.selector).cancel();
		}
	}
	
	private void write(SelectionKey key) throws IOException {
		final UnixSocketChannel channel = this.getChannel(key);
		
		synchronized (this.pendingData) {
			List<ByteBuffer> queue = (List<ByteBuffer>) this.pendingData.get(channel);
			System.out.println(new String(queue.get(0).array()));

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
				// We wrote away all data, so we're no longer interested in writing on this socket. Switch back to waiting for data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		final UnixSocketChannel channel = this.getChannel(key);
	
		// Finish the connection. If the connection operation failed this will raise an IOException.
		try {
			System.out.println(channel.finishConnect());
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			LOG.debug(e.getMessage(), e);
			key.cancel();
			return;
		}
	
		// Register an interest in writing on this channel
		//this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		//key.interestOps(SelectionKey.OP_WRITE);
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
            	LOG.debug(String.format("error: socket \"%s\" does not exist after %s retry.", socketFile.getAbsolutePath(), retries));
            }
        }
		this.address = new UnixSocketAddress(socketFile.getAbsoluteFile());	
		this.channel = UnixSocketChannel.open(this.address);
		this.channel.configureBlocking(false);
		// synchronize pending changes
		synchronized(this.pendingChanges) {
			this.pendingChanges.add(new ChangeRequest(channel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE));
		}
		return this.channel;
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
