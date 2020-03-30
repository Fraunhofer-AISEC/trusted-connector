package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsConstants;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A TLS Server that listens on a given port from the IDSCPv2Settings and create new
 * TLSServerThreads for incoming connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServer extends Thread implements SecureServer {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServer.class);

    private volatile boolean isRunning = false;
    private ServerSocket serverSocket;
    private IDSCPv2Callback idscpConfigCallback; //no race conditions
    private IdscpConnectionListener idscpServerCallback;

    public TLSServer(IDSCPv2Settings serverSettings, IDSCPv2Callback configCallback,
                     IdscpConnectionListener idscpServerCallback)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        this.idscpConfigCallback = configCallback;
        this.idscpServerCallback = idscpServerCallback;

        /* init server for TCP/TLS communication */

        /* get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
         * hostVerification and algorithm constraints */
        LOG.debug("Create trust manager for tls server");
        TrustManager[] myTrustManager = PreConfiguration.getX509ExtTrustManager(
                serverSettings.getTrustStorePath(),
                serverSettings.getTrustStorePassword()
        );

        /* get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
         * connection specific key selection via key alias*/
        LOG.debug("Create key manager for tls server");
        KeyManager[] myKeyManager = PreConfiguration.getX509ExtKeyManager(
                serverSettings.getKeyStorePath(),
                serverSettings.getKeyStorePassword(),
                serverSettings.getCertAlias(),
                serverSettings.getKeyStoreKeyType()
        );

        LOG.debug("Set tls security attributes and create tls server socket");

        // create tls context based on keyManager and trustManager
        SSLContext sslContext = SSLContext.getInstance(TlsConstants.TLS_INSTANCE);
        sslContext.init(myKeyManager, myTrustManager, null);
        SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

        //create server socket
        serverSocket =  socketFactory.createServerSocket(serverSettings.getServerPort());
        //set timeout for serverSocket.accept() to allow safeStop()
        serverSocket.setSoTimeout(5000);

        SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;

        //set tls constraints
        SSLParameters sslParameters = sslServerSocket.getSSLParameters();
        sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
        sslParameters.setNeedClientAuth(true); //client must authenticate
        sslParameters.setProtocols(TlsConstants.TLS_ENABLED_PROTOCOLS); //only TLSv1.3
        sslParameters.setCipherSuites(TlsConstants.TLS_ENABLED_CIPHER_TLS13); //only allow strong cipher suite
        sslServerSocket.setSSLParameters(sslParameters);
        LOG.debug("TLS server was initialized successfully");
    }

    @Override
    public void run(){
        LOG.debug("Starting tls server");

        if (serverSocket == null || serverSocket.isClosed()){
            LOG.error("TLS Server socket is not available");
            return;
        }

        SSLSocket sslSocket;
        isRunning = true;
        LOG.debug("TLS server is running");
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
                LOG.error("TLS Server failed");
                e.printStackTrace();
                isRunning = false;
                return;
            }

            //start new server thread
            LOG.debug("New TLS client has connected. Create new server session");
            TLSServerThread server = new TLSServerThread(sslSocket, idscpConfigCallback, idscpServerCallback);
            sslSocket.addHandshakeCompletedListener(server);
            server.start();
        }

        if (serverSocket != null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.warn("Could not close TLS server socket");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void safeStop(){
        LOG.debug("Stopping tls server");
        isRunning = false;
    }

    @Override
    public boolean isRunning(){
        return isRunning;
    }

}
