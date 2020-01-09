package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

/**
 * A TLSServerThread that notifies an IDSCPv2Config when a secure channel was created and the TLS handshake is done
 * When new data are available the serverThread transfers them to the SecureChannelListener
 *
 * Developer API
 *
 * constructors:
 * TLSServerThread(IDSCPv2Settings, IDSCPv2Callback) initializes the TLS Socket
 *
 * Methods:
 * run()    runs the serverThread and starts listening for new data
 *
 * close()  disconnects the serverThread
 *
 * setConnectionId(ConnectionId) set the internal connectionId, which is used for notifying the IDSCPv2Configuration
 *                                when the client quits the connection
 *
 * handshakeCompleted()        create a secureChannel, including this serverThread and provides it to the IDSCPv2Config
 *
 * send(byte[] data)            send data to the client
 *
 * onMessage(int len, byte[] rawData) is called when new data are available. Transfer them to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServerThread extends Thread implements HandshakeCompletedListener, SecureChannelEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServerThread.class);

    private SSLSocket sslSocket;
    private volatile boolean running = true;
    private InputStream in;
    private OutputStream out;
    private String connectionId;
    private SecureChannelListener listener = null;
    private IDSCPv2Callback callback;
    private CountDownLatch listenerLatch = new CountDownLatch(1);


    TLSServerThread(SSLSocket sslSocket, IDSCPv2Callback callback){
        this.sslSocket = sslSocket;
        this.callback = callback;

        try {
            //set timout for blocking read
            sslSocket.setSoTimeout(5000);
            in = sslSocket.getInputStream();
            out = sslSocket.getOutputStream();
        } catch (IOException e){
            LOG.error(e.getMessage());
            running = false;
        }
    }

    @Override
    public void setConnectionId(String connectionId){
        this.connectionId = connectionId;
    }

    @Override
    public void run(){
        //wait for new data while running
        byte[] buf = new byte[2048];

        while (running){
            try {
                int len = in.read(buf, 0, buf.length);
                if (0 > len) {
                    //onMessage(TlsConstants.END_OF_STREAM.length(), TlsConstants.END_OF_STREAM.getBytes());
                    running = false;
                } else {
                    onMessage(len, buf);
                }
            } catch (SocketTimeoutException e){
                //timeout catches safeStop() call and allows to send server_goodbye
                //alternative: close sslSocket and catch SocketException
                //continue
            } catch (SSLException e){
                LOG.error("SSL error");
                e.printStackTrace();
                running = false;
                return;
            } catch (IOException e){
                e.printStackTrace();
                running = false;
            }
        }
        callback.connectionClosedHandler(this.connectionId);
        LOG.trace("ServerThread is terminating");
        try {
            out.close();
            in.close();
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] data) {
        if (!isConnected()){
            LOG.error("Server cannot send data because socket is not connected");
        } else {
            try {
                out.write(data);
                out.flush();
                LOG.trace("Send message: " + new String(data));
            } catch (IOException e){
                LOG.error("Server cannot send data");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        safeStop();
    }

    public void onMessage(int len, byte[] rawData)  {
        byte[] data = new byte[len];
        System.arraycopy(rawData, 0, data, 0, len);
        try{
            listenerLatch.await();
            this.listener.onMessage(data);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void safeStop(){
        running = false;
    }

    public boolean isConnected() {
        return (sslSocket != null && sslSocket.isConnected());
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        LOG.debug("TLS handshake was successful");
        SecureChannel secureChannel = new SecureChannel(this);
        this.listener = secureChannel;
        listenerLatch.countDown();
        callback.secureChannelListenHandler(secureChannel);
    }
}
