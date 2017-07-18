package de.fhg.ids.cm.impl.trustx;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.unixsocket.UnixSocketChannel;

public class TrustXMockHandler implements Runnable{
	private Logger LOG = LoggerFactory.getLogger(TrustXMockHandler.class);
	private List<ServerDataEvent> queue = new LinkedList<>();
	
	@Override
	public void run() {
	    ServerDataEvent dataEvent;
	    
	    while(true) {
	      // Wait for data to become available
	      synchronized(queue) {
	        while(queue.isEmpty()) {
	          try {
	            queue.wait();
	          } catch (InterruptedException e) {
	          }
	        }
	        dataEvent = (ServerDataEvent) queue.remove(0);
	      }
	      
	      // Print
	      System.out.println(new String(dataEvent.data));
	      dataEvent.server.send(dataEvent.socket, dataEvent.data);
	    }
	  }
	
	public void handleResponse(TrustXMock server, UnixSocketChannel socket, byte[] data, int count) {
	    byte[] dataCopy = new byte[count];
	    System.arraycopy(data, 0, dataCopy, 0, count);
	    synchronized(queue) {
	      queue.add(new ServerDataEvent(server, socket, dataCopy));
	      queue.notify();
	    }
	}
}

