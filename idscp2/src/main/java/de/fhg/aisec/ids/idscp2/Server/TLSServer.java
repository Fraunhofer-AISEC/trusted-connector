package de.fhg.aisec.ids.idscp2.Server;

import de.fhg.aisec.ids.idscp2.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * A TLS server implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSServer extends IDSCPv2Server {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServer.class);

    public TLSServer(ServerConfiguration serverConfiguration){
        this.serverConfiguration = serverConfiguration;

        /* init server for TCP/TLS communication */
        try {
            //toDo create and load KeyManager and TrustManager
            TrustManager[] myTrustManager = null;
            KeyManager[] myKeyManager = null;

            SSLContext sslContext = SSLContext.getInstance(Constants.TLS_INSTANCE);
            sslContext.init(myKeyManager, myTrustManager, null);

            SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

            //create server socket
            serverSocket =  socketFactory.createServerSocket(serverConfiguration.getServerPort());

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

                //set SSL Parameters
                SSLParameters sslParameters = sslSocket.getSSLParameters();
                sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
                sslParameters.setNeedClientAuth(true); //client must authenticate

                //start handshake
                sslSocket.startHandshake();

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


    /*InputStream jksKeyStoreInputStream =
          Files.newInputStream(targetDirectory.resolve(keyStoreName));
      InputStream jksTrustStoreInputStream =
          Files.newInputStream(targetDirectory.resolve(trustStoreName));

      KeyStore keystore = KeyStore.getInstance("JKS");
      KeyStore trustManagerKeyStore = KeyStore.getInstance("JKS");

      LOG.info("Loading key store: " + keyStoreName);
      LOG.info("Loading trus store: " + trustStoreName);
      keystore.load(jksKeyStoreInputStream, keyStorePassword.toCharArray());
      trustManagerKeyStore.load(jksTrustStoreInputStream, keyStorePassword.toCharArray());
      java.security.cert.Certificate[] certs = trustManagerKeyStore.getCertificateChain("ca");
      LOG.info("Cert chain: " + Arrays.toString(certs));

      LOG.info("LOADED CA CERT: " + trustManagerKeyStore.getCertificate("ca"));
      jksKeyStoreInputStream.close();
      jksTrustStoreInputStream.close();

        * // get private key
            Key privKey = (PrivateKey) keystore.getKey(keystoreAliasName, keyStorePassword.toCharArray());
            // Get certificate of public key
            X509Certificate cert = (X509Certificate) keystore.getCertificate(keystoreAliasName);

            TrustManager[] trustManagers;
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustManagerKeyStore);
            trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException(
                        "Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
        */

    /*
    * @Override
public void close() throws IOException {
  try (
    Socket       s = socket;
    InputStream  i = in;
    OutputStream o = out;
  ) {
    sendMessage(Message.CLIENT_CLOSE);
  }
}*/
}
