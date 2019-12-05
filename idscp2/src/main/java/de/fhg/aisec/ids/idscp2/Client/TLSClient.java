package de.fhg.aisec.ids.idscp2.Client;

import de.fhg.aisec.ids.idscp2.Constants;
import de.fhg.aisec.ids.idscp2.InputListenerThread;
import de.fhg.aisec.ids.idscp2.TLSPreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A TLS client implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSClient extends IDSCPv2Client implements HandshakeCompletedListener{
    private static final Logger LOG = LoggerFactory.getLogger(TLSClient.class);

    public TLSClient(ClientConfiguration clientConfiguration){
        this.clientConfiguration = clientConfiguration;

        /* init TLS Client */
        try {
            /* get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
             * hostVerification and algorithm constraints */
            TrustManager[] myTrustManager = TLSPreConfiguration.getX509ExtTrustManager(
                    clientConfiguration.getTrustStorePath(),
                    clientConfiguration.getTrustStorePassword()
            );

            /* get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
             * connection specific key selection via key alias*/
            KeyManager[] myKeyManager = TLSPreConfiguration.getX509ExtKeyManager(
                    clientConfiguration.getKeyStorePath(),
                    clientConfiguration.getKeyStorePassword(),
                    clientConfiguration.getCertAlias(),
                    clientConfiguration.getKeyStoreKeyType()
            );

            SSLContext sslContext = SSLContext.getInstance(Constants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            //create server socket
            clientSocket = socketFactory.createSocket();

            SSLSocket sslSocket = (SSLSocket) clientSocket;
            SSLParameters sslParameters = sslSocket.getSSLParameters();
            sslParameters.setUseCipherSuitesOrder(false); //use server priority order
            sslParameters.setNeedClientAuth(true);
            sslParameters.setProtocols(Constants.TLS_ENABLED_PROTOCOLS); //only TLSv1.2
            sslParameters.setCipherSuites(Constants.TLS_ENABLED_CIPHER); //only allow strong cipher
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); //use https for hostname verification
            sslSocket.setSSLParameters(sslParameters);
            LOG.info("TLS Client was initialized successfully");

        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e){
            LOG.error("Init TLS Client failed");
            e.printStackTrace();
        }
    }

    @Override
    public boolean connect() {
        SSLSocket sslSocket = (SSLSocket) clientSocket;
        if (sslSocket == null || sslSocket.isClosed()){
            System.out.println("Client socket is not available");
            return false;
        }

        try {
            sslSocket.connect(new InetSocketAddress(clientConfiguration.getHostname(),
                    clientConfiguration.getServerPort()));
            LOG.info("Client is connected to server " + clientConfiguration.getHostname()
                    + ":" + clientConfiguration.getServerPort());

            //set clientSocket timeout to allow safeStop()
            clientSocket.setSoTimeout(5000);

            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();

            //add inputListener but start it not before handshake is complete
            inputListenerThread = new InputListenerThread(in);
            inputListenerThread.register(this);

            //start tls handshake
            sslSocket.addHandshakeCompletedListener(this);
            LOG.info("Start TLS Handshake");
            sslSocket.startHandshake();
        } catch (SSLHandshakeException e){
            System.out.println("TLS Handshake failed" + e.getMessage());
            disconnect(false);
            return false;
        } catch (IOException e) {
            LOG.error("Connecting TLS client to server failed");
            //e.printStackTrace();
            disconnect(false);
            return false;
        }
        return true;
    }

    public SSLSession getSslSession() {
        return ((SSLSocket)clientSocket).getSession();
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        //start receiving listener for IDSCPv2 communication after TLS Handshake was successful
        LOG.info("TLS Handshake was successful. Starting input listener thread");
        inputListenerThread.start();
    }
}
