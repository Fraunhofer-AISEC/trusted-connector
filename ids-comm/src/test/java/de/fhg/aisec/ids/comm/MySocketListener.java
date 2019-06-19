package de.fhg.aisec.ids.comm;

import de.fhg.aisec.ids.comm.server.IdscpServerSocket;
import de.fhg.aisec.ids.comm.server.SocketListener;
import org.eclipse.jetty.websocket.api.Session;

class MySocketListener implements SocketListener {
  private String lastMsg = null;

  @Override
  public synchronized void onMessage(Session session, byte[] msg) {
    // Wake Thread(s) that called getLastMsg()
    this.notifyAll();
    this.lastMsg = new String(msg);
  }

  synchronized String getLastMsg() {
    // If message is null, we wait for asynchronous delivery
    if (this.lastMsg == null) {
      try {
        this.wait(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return this.lastMsg;
  }

  @Override
  public void notifyClosed(IdscpServerSocket idscpServerSocket) {
    // Nothing to do here. Socket is already closed.
  }
}
