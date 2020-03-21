package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsConstants;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

/**
 * A TLS Client that notifies an IDSCPv2Configuration when a secure channel was created and the TLS handshake is done
 * The client is notified from an InputListenerThread when new data are available and transfer them to the
 * SecureChannelListener
 *
 * Developer API
 *
 * constructors:
 * TLSClient(IDSCPv2Settings, IDSCPv2Callback) initializes the TLS Socket and all TLS Security configurations like
 *                                              sslParameters (protocol, cipher, ..), trustStore, keyStore
 *
 * Methods:
 * connect(String host, int port) connect the client asynchronous to the server and starts the TLS handshake
 *
 * close()  disconnects the client
 *
 *
 * handshakeCompleted()         create a secureChannel, including this client; start inputListenerThread and transfer
 *                              the secureChannel to the IDSCPv2Configuration
 *
 * send(byte[] data)            send data to the server
 *
 * onMessage(int len, byte[] rawData)   is a callback function for the InputListenerThread, that is called when new
 *                                      data are available. Transfer them to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSClient implements HandshakeCompletedListener, DataAvailableListener, SecureChannelEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TLSClient.class);

    private Socket clientSocket;
    private DataOutputStream out;
    private InputListenerThread inputListenerThread;
    private SecureChannelListener listener; //race conditions are avoided using CountDownLatch
    private CountDownLatch listenerLatch = new CountDownLatch(1);
    private IDSCPv2Callback callback; //race conditions are avoided because callback is initialized in constructor


    public TLSClient(IDSCPv2Settings clientSettings, IDSCPv2Callback callback)
            throws IOException, KeyManagementException, NoSuchAlgorithmException{
        this.callback = callback;
        /* init TLS Client */

        /* get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
         * hostVerification and algorithm constraints */
        TrustManager[] myTrustManager = PreConfiguration.getX509ExtTrustManager(
                clientSettings.getTrustStorePath(),
                clientSettings.getTrustStorePassword()
        );

        /* get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
         * connection specific key selection via key alias*/
        KeyManager[] myKeyManager = PreConfiguration.getX509ExtKeyManager(
                clientSettings.getKeyStorePath(),
                clientSettings.getKeyStorePassword(),
                clientSettings.getCertAlias(),
                clientSettings.getKeyStoreKeyType()
        );

        SSLContext sslContext = SSLContext.getInstance(TlsConstants.TLS_INSTANCE);
        sslContext.init(myKeyManager, myTrustManager, null);

        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        //create server socket
        clientSocket = socketFactory.createSocket();

        SSLSocket sslSocket = (SSLSocket) clientSocket;
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setUseCipherSuitesOrder(false); //use server priority order
        sslParameters.setNeedClientAuth(true);
        sslParameters.setProtocols(TlsConstants.TLS_ENABLED_PROTOCOLS); //only TLSv1.2
        sslParameters.setCipherSuites(TlsConstants.TLS_ENABLED_CIPHER_TLS13); //only allow strong cipher
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); //use https for hostname verification
        sslSocket.setSSLParameters(sslParameters);
        LOG.debug("TLS Client was initialized successfully");
    }


    public void connect(String hostname, int port){
        SSLSocket sslSocket = (SSLSocket) clientSocket;
        if (sslSocket == null || sslSocket.isClosed()){
            LOG.warn("Client socket is not available");
            callback.secureChannelConnectHandler(null);
            return;
        }

        try {
            sslSocket.connect(new InetSocketAddress(hostname,
                    port));
            LOG.debug("Client is connected to server {}:{}", hostname, port);

            //set clientSocket timeout to allow safeStop()
            clientSocket.setSoTimeout(5000);

            out = new DataOutputStream(clientSocket.getOutputStream());

            //add inputListener but start it not before handshake is complete
            inputListenerThread = new InputListenerThread(clientSocket.getInputStream());
            inputListenerThread.register(this);

            //start tls handshake
            sslSocket.addHandshakeCompletedListener(this);
            LOG.debug("Start TLS Handshake");
            sslSocket.startHandshake();
        } catch (SSLHandshakeException | SSLProtocolException e){
            LOG.warn("TLS Handshake failed: {}", e.getMessage());
            disconnect();
            callback.secureChannelConnectHandler(null);
        } catch (IOException e) {
            LOG.error("Connecting TLS client to server failed " + e.getMessage());
            disconnect();
            callback.secureChannelConnectHandler(null);
        }
    }

    private void disconnect(){
        LOG.debug("Disconnecting from tls server");
        //close listener
        if (inputListenerThread != null && inputListenerThread.isAlive()) {
            inputListenerThread.safeStop();
        }

        if (clientSocket != null && !clientSocket.isClosed()){
            try {
                clientSocket.close();
            } catch (IOException e) {
                //nothing to do
            }
        }
    }

    @Override
    public void onClose(){
        try {
            listenerLatch.await();
            listener.onClose();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onError(){
        try {
            listenerLatch.await();
            listener.onError();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    public void send(byte[] data){
        if (!isConnected()){
            LOG.error("Client cannot send data because socket is not connected");
            onError();
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.debug("Send message: {}", new String(data));
            } catch (IOException e){
                LOG.error("Client cannot send data");
                onError();
            }
        }
    }

    public boolean isConnected(){
        return clientSocket != null && clientSocket.isConnected();
    }

    public SSLSession getSslSession() {
        return ((SSLSocket)clientSocket).getSession();
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        //start receiving listener after TLS Handshake was successful
        LOG.debug("TLS Handshake was successful. Starting input listener thread");
        SecureChannel secureChannel = new SecureChannel(this);
        this.listener = secureChannel;
        listenerLatch.countDown();
        inputListenerThread.start();
        callback.secureChannelConnectHandler(secureChannel);
    }

    @Override
    public void onMessage(byte[] data) {
        try{
            listenerLatch.await();
            listener.onMessage(data);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
