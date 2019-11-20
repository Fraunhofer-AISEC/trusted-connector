package de.fhg.aisec.ids.idscp2.Client;

import de.fhg.aisec.ids.idscp2.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A TLS client implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSClient extends IDSCPv2Client {
    private static final Logger LOG = LoggerFactory.getLogger(TLSClient.class);

    public TLSClient(ClientConfiguration clientConfiguration){
        this.clientConfiguration = clientConfiguration;

        /* init TLS Client */
        try {
            //create and load KeyManager and TrustManager
            TrustManager[] myTrustManager = null;
            KeyManager[] myKeyManager = null;

            SSLContext sslContext = SSLContext.getInstance(Constants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();

            //create server socket
            clientSocket = socketFactory.createSocket();

            //toDo set sslParameters
            SSLParameters sslParameters = ((SSLSocket)clientSocket).getSSLParameters();


        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e){
            LOG.error("Init TLS Client failed");
            e.printStackTrace();
        }
    }

    @Override
    public boolean connect() {
        SSLSocket sslSocket = (SSLSocket) clientSocket;
        if (super.connect()){
            try{
                sslSocket.startHandshake();
            } catch (IOException e){
                LOG.error("Starting TLS handshake failed");
                e.printStackTrace();
                disconnect();
                return false;
            }
            return true;
        }
        return false;
    }

    public SSLSession getSslSession() {
        return ((SSLSocket)clientSocket).getSession();
    }
}
