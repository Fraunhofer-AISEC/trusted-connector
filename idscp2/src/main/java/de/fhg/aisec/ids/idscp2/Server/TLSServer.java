package de.fhg.aisec.ids.idscp2.Server;

import de.fhg.aisec.ids.idscp2.Constants;
import de.fhg.aisec.ids.idscp2.TLSPreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;

/**
 * A TLS server implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSServer extends IDSCPv2Server {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServer.class);

    public TLSServer(ServerConfiguration serverConfiguration){
        super(serverConfiguration);

        /* init server for TCP/TLS communication */
        try {
            /* get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
            * hostVerification and algorithm constraints */
            TrustManager[] myTrustManager = TLSPreConfiguration.getX509ExtTrustManager(
                    serverConfiguration.getTrustStorePath(),
                    serverConfiguration.getTrustStorePassword()
            );

            /* get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
             * connection specific key selection via key alias*/
            KeyManager[] myKeyManager = TLSPreConfiguration.getX509ExtKeyManager(
                    serverConfiguration.getKeyStorePath(),
                    serverConfiguration.getKeyStorePassword()
            );

            // create tls context based on keyManager and trustManager
            SSLContext sslContext = SSLContext.getInstance(Constants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);
            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

            //create server socket
            serverSocket =  socketFactory.createServerSocket(serverConfiguration.getServerPort());

            SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
            SSLParameters sslParameters = sslServerSocket.getSSLParameters();
            sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
            sslParameters.setNeedClientAuth(true); //client must authenticate
            //toDo set further SSL Parameters e.g SNI Matchers, Cipher Suite, Protocols ...
            sslServerSocket.setSSLParameters(sslParameters);

        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e){
            LOG.error("Init SSL server socket failed");
            e.printStackTrace();
        }
    }

    @Override
    public boolean start() {
        if (serverSocket == null || serverSocket.isClosed()){
            LOG.error("SSL Server socket is not available");
            return false;
        }

        //run server
        super.start();

        try {
            while(isRunning){
                SSLSocket sslSocket = (SSLSocket) serverSocket.accept();

                //start new thread
                SSLServerThread server = new SSLServerThread(sslSocket);
                //server.setName();
                servers.add(server);
                server.start();
            }

        } catch (IOException e) {
            LOG.error("SSL Server failed");
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
