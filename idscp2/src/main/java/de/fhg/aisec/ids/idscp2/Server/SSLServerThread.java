package de.fhg.aisec.ids.idscp2.Server;

import javax.net.ssl.SSLSocket;

/**
 * A TLS server thread implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class SSLServerThread extends Thread implements ServerThread{

    private SSLSocket sslSocket = null;
    private volatile boolean running = true;

    SSLServerThread(SSLSocket sslSocket){
        this.sslSocket = sslSocket;
    }

    @Override
    public void run(){
        while (running){
            //do something

        }
    }

    public void safeStop(){
        running = false;
    }
}
