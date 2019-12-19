package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsConstants;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.keystores.TLSPreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A TLS Server that listens on a given port from the IDSCPv2Settings and create new TLSServerThreads for incoming
 * connections
 *
 * Developer API
 *
 * constructors:
 * TLSServer(IDSCPv2Settings, IDSCPv2Callback) initializes the TLS Socket and all TLS Security configurations like
 *                                              sslParameters (protocol, cipher, ..), trustStore, keyStore
 *
 * Methods:
 * run()
 * safeStop()
 * isRunning()
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServer extends Thread implements SecureServer {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServer.class);

    private volatile boolean isRunning = false;
    private ServerSocket serverSocket = null;
    private IDSCPv2Callback callback;

    public TLSServer(IDSCPv2Settings serverSettings, IDSCPv2Callback callback){
        this.callback = callback;

        /* init server for TCP/TLS communication */
        try {
            /* get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
             * hostVerification and algorithm constraints */
            TrustManager[] myTrustManager = TLSPreConfiguration.getX509ExtTrustManager(
                    serverSettings.getTrustStorePath(),
                    serverSettings.getTrustStorePassword()
            );

            /* get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
             * connection specific key selection via key alias*/
            KeyManager[] myKeyManager = TLSPreConfiguration.getX509ExtKeyManager(
                    serverSettings.getKeyStorePath(),
                    serverSettings.getKeyStorePassword(),
                    serverSettings.getCertAlias(),
                    serverSettings.getKeyStoreKeyType()
            );

            // create tls context based on keyManager and trustManager
            SSLContext sslContext = SSLContext.getInstance(TlsConstants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);
            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

            //create server socket
            serverSocket =  socketFactory.createServerSocket(serverSettings.getServerPort());
            //set timeout for serverSocket.accept() to allow safeStop()
            serverSocket.setSoTimeout(5000);

            SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
            SSLParameters sslParameters = sslServerSocket.getSSLParameters();
            sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
            sslParameters.setNeedClientAuth(true); //client must authenticate
            sslParameters.setProtocols(TlsConstants.TLS_ENABLED_PROTOCOLS); //only TLSv1.3
            sslParameters.setCipherSuites(TlsConstants.TLS_ENABLED_CIPHER_TLS13); //only allow strong cipher suite
            //FIXME uncomment hostname identification, this is deactivated because the client uses a certificate of an other identity in the examples at the moment
            //sslParameters.setEndpointIdentificationAlgorithm("HTTPS"); //use https for hostname verification
            sslServerSocket.setSSLParameters(sslParameters);
            LOG.info("Server was initialized successfully");

        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e){
            LOG.error("Init SSL server socket failed");
            e.printStackTrace();
        }

    }

    @Override
    public void run(){
        LOG.info("Starting server");

        if (serverSocket == null || serverSocket.isClosed()){
            LOG.error("TLS Server socket is not available");
            return;
        }

        SSLSocket sslSocket;
        isRunning = true;
        LOG.info("Server is running");
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
                System.out.println("TLS Server failed");
                e.printStackTrace();
                isRunning = false;
                return;
            }

            //start new server thread
            LOG.info("New TLS client has connected. Create new server session");
            TLSServerThread server = new TLSServerThread(sslSocket, this.callback);
            sslSocket.addHandshakeCompletedListener(server);
            server.start();
        }

        if (serverSocket != null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.error("Could not close TLS server socket");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void safeStop(){
        LOG.info("Stopping server");
        isRunning = false;
    }

    @Override
    public boolean isRunning(){
        return isRunning;
    }

}
