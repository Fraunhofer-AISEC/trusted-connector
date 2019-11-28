package de.fhg.aisec.ids.idscp2.Client;

import de.fhg.aisec.ids.idscp2.Constants;
import de.fhg.aisec.ids.idscp2.InputListenerThread;
import de.fhg.aisec.ids.idscp2.TLSPreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ConnectException;
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
                    clientConfiguration.getKeyStorePassword()
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
            //toDo set further SSL Parameters e.g SNI Matchers, Cipher Suite, Protocols ... whatever
            sslSocket.setSSLParameters(sslParameters);

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
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();

            //start tls handshake
            sslSocket.addHandshakeCompletedListener(this);
            sslSocket.startHandshake();
        } catch (IOException e) {
            LOG.error("Connecting TLS client to server failed");
            //e.printStackTrace();
            disconnect();
            return false;
        }
        return true;
    }

    public SSLSession getSslSession() {
        return ((SSLSocket)clientSocket).getSession();
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        //start receiving listener for IDSCPv2 communication
        inputListenerThread = new InputListenerThread(in);
        inputListenerThread.register(this);
        inputListenerThread.start();
    }
}
