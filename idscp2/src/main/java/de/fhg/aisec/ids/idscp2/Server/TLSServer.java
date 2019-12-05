package de.fhg.aisec.ids.idscp2.Server;

import de.fhg.aisec.ids.idscp2.Constants;
import de.fhg.aisec.ids.idscp2.TLSPreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.*;
import java.util.UUID;

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
                    serverConfiguration.getKeyStorePassword(),
                    serverConfiguration.getCertAlias(),
                    serverConfiguration.getKeyStoreKeyType()
            );

            // create tls context based on keyManager and trustManager
            SSLContext sslContext = SSLContext.getInstance(Constants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);
            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

            //create server socket
            serverSocket =  socketFactory.createServerSocket(serverConfiguration.getServerPort());
            //set timeout for serverSocket.accept() to allow safeStop()
            serverSocket.setSoTimeout(5000);

            SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
            SSLParameters sslParameters = sslServerSocket.getSSLParameters();
            sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
            sslParameters.setNeedClientAuth(true); //client must authenticate
            sslParameters.setProtocols(Constants.TLS_ENABLED_PROTOCOLS); //only TLSv1.2
            sslParameters.setCipherSuites(Constants.TLS_ENABLED_CIPHER); //only allow strong cipher suite
            //toDo uncomment hostname identification, this is deactivated because the client uses a certificate of an other identity in the examples at the moment
            //sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); //use https for hostname verification
            sslServerSocket.setSSLParameters(sslParameters);
            LOG.info("Server was initialized successfully");

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

        SSLSocket sslSocket;
        while(isRunning){
            try {
                sslSocket = (SSLSocket) serverSocket.accept();
            } catch (SocketTimeoutException e){
                //timeout on serverSocket blocking functions was reached
                //in this way we can catch safeStop() function, that makes isRunning false
                //without closing the serverSocket, so we can stop and restart the server
                //alternative: close serverSocket. But then we cannot reuse it
                continue;
            } catch (IOException e) {
                System.out.println("SSL Server failed");
                e.printStackTrace();
                return false;
            }

            //start new server thread
            LOG.info("New client has connected. Create new server session");
            String connectionId = UUID.randomUUID().toString();
            TLSServerThread server = new TLSServerThread(sslSocket, connectionId);
            server.registerListener(this);
            servers.put(connectionId, server);
            sslSocket.addHandshakeCompletedListener(server);
            server.start();
        }

        return true;
    }
}
