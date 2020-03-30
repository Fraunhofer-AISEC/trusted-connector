package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsConstants;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsSessionVerificationHelper;
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
 * A TLS Client that notifies an IDSCPv2Configuration when a secure channel was created and the
 * TLS handshake is done. The client is notified from an InputListenerThread when new data are
 * available and transfer it to the SecureChannelListener
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

        //set TLS constraints
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setUseCipherSuitesOrder(false); //use server priority order
        sslParameters.setNeedClientAuth(true);
        sslParameters.setProtocols(TlsConstants.TLS_ENABLED_PROTOCOLS); //only TLSv1.3
        sslParameters.setCipherSuites(TlsConstants.TLS_ENABLED_CIPHER_TLS13); //only allow strong cipher
        //sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); is done in application layer
        sslSocket.setSSLParameters(sslParameters);
        LOG.debug("TLS Client was initialized successfully");
    }


    /*
     * Connect to TLS server and start TLS Handshake
     */
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

    @Override
    public boolean send(byte[] data){
        if (!isConnected()){
            LOG.error("Client cannot send data because socket is not connected");
            return false;
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.debug("Send message");
                return true;
            } catch (IOException e){
                LOG.error("Client cannot send data");
                return false;
            }
        }
    }

    public boolean isConnected(){
        return clientSocket != null && clientSocket.isConnected();
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        //start receiving listener after TLS Handshake was successful
        if (LOG.isDebugEnabled()) {
            LOG.debug("TLS Handshake was successful");
        }

        // verify tls session on application layer: hostname verification, certificate validity
        try {
            TlsSessionVerificationHelper.verifyTlsSession(handshakeCompletedEvent.getSession());
            LOG.debug("TLS session is valid");
        } catch (SSLPeerUnverifiedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("TLS session is not valid. Close TLS connection", e);
            }
            disconnect();
            callback.secureChannelConnectHandler(null);
            return;
        }

        //Create secure channel, register secure channel as message listener and provide it to
        // IDSCPv2 Configuration.
        //Start Input Listener
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
