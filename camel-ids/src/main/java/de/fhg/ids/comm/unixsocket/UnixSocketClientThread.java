package de.fhg.ids.comm.unixsocket;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
/**
 * Thread for handling incoming and outgoing messages 
 * over a unix socket
 * 
 * @author Georg Räß (georg.raess@aisec.fraunhofer.de)
 *
 */
public class UnixSocketClientThread extends Thread {
	private Logger LOG = LoggerFactory.getLogger(UNIXReadWriteDispatcher.class);
	private boolean socketOpen = false;
	private UnixSocketAddress address;
	private UnixSocketChannel channel;
	
	public boolean openSocket() {
		// open socket file
		java.io.File socketFile = new java.io.File(Dispatcher.SOCKET);
		// Try to open socket file 10 times
		int retries = 1;
        while (!socketFile.exists()) {
            try {
				TimeUnit.MILLISECONDS.sleep(250L);
			} catch (InterruptedException e) {
				LOG.info("InterruptedException");
				e.printStackTrace();
			}
            retries++;
            if (retries > 11) {
            	LOG.info(String.format("socket %s does not exist after %s retries.", socketFile.getAbsolutePath(), retries));
            	return false;
            }
        }
		this.address = new UnixSocketAddress(socketFile);
		return true;
	}
	
	@Override
	public void run() {
		super.setName("UnixSocketThread-"+this.getId());
		if(!this.socketOpen){
			this.socketOpen = this.openSocket();
		}
		else {
			try {
				// Start listening on internal interface (UNIX domain socket)
				Selector sel = NativeSelectorProvider.getInstance().openSelector();
				channel = UnixSocketChannel.open(this.address);
				channel.configureBlocking(false);
				LOG.info("connected to " + channel.getRemoteSocketAddress());
				channel.register(sel, (SelectionKey.OP_READ | SelectionKey.OP_WRITE), new UNIXReadWriteDispatcher(channel));
				
				while (true) {
					int readyChannels = sel.select();
					
					if (readyChannels == 0)
						continue;
					
					Set<SelectionKey> keys = sel.selectedKeys();
					
					Iterator<SelectionKey> keyIterator = keys.iterator();
					while (keyIterator.hasNext()) {
						SelectionKey k = keyIterator.next();
						Dispatcher d = (Dispatcher) k.attachment();
						keyIterator.remove();
						if ((!d.receive() || !d.dispatch()) && d instanceof UNIXReadWriteDispatcher) {
							LOG.info("Removed a client read/write dispatcher");
							k.cancel();
						}
					}
				}

			} catch (IOException e) {
				LOG.error(e.getMessage(),  e);
			}
		}
	}	
}
