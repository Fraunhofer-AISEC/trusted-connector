package de.fhg.aisec.ids.idscp2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.lang.reflect.Array;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * A custom X590ExtendedKeyManager, that allows to choose a TrustStore entry by a given certificate alias and
 * delegates all other function calls to given default X509ExtendedKeyManager
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class CustomX509ExtendedKeyManager extends X509ExtendedKeyManager{
    private static final Logger LOG = LoggerFactory.getLogger(CustomX509ExtendedKeyManager.class);

    private final String certAlias;
    private final String keyType;
    private final X509ExtendedKeyManager delegate;

    public CustomX509ExtendedKeyManager(String alias, String keyType, final X509ExtendedKeyManager delegate){
        super();
        this.certAlias = alias;
        this.keyType = keyType;
        this.delegate = delegate;
    }

    @Override
    public String[] getClientAliases(String s, Principal[] principals) {
        return delegate.getClientAliases(s, principals);
    }

    @Override
    /* returns an existing certAlias that matches one of the given KeyTypes, or null;
       called only by client in TLS handshake */
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        if (Arrays.asList(keyTypes).contains(this.keyType)){
            if (Arrays.asList(getServerAliases(keyType, issuers)).contains(this.certAlias)) {
                LOG.info("CertificateAlias is {}", this.certAlias);
                return this.certAlias;
            } else {
                LOG.warn("certAlias '{}' was not found in keystore", this.certAlias);
                return null;
            }
        } else {
            LOG.warn("Invalid keyType '{}' in chooseClientAlias() in class X509ExtendedKeyManager",
                    Arrays.toString(keyTypes));
            LOG.warn("Expected: '{}'", this.keyType);
            return null;
        }
    }

    @Override
    public String[] getServerAliases(String s, Principal[] principals) {
        return delegate.getServerAliases(s, principals);
    }

    @Override
    /* returns an existing certAlias that matches the given KeyType, or null;
       called only by server in TLS handshake*/
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (keyType.equals(this.keyType)){
            if (Arrays.asList(getServerAliases(keyType, issuers)).contains(this.certAlias)) {
                LOG.info("CertificateAlias is {}", this.certAlias);
                return this.certAlias;
            } else {
                LOG.warn("certAlias '{}' was not found in keystore", this.certAlias);
                return null;
            }
        } else {
            LOG.warn("Invalid keyType '{}' in chooseServerAlias() in class X509ExtendedKeyManager", keyType);
            LOG.warn("Expected: '{}'", this.keyType);
            return null;
        }
    }

    @Override
    /* returns the certificate chain of a given certificateAlias;
       called by client and server in TLS Handshake after alias was chosen */
    public X509Certificate[] getCertificateChain(String certAlias) {
        if (certAlias.equals(this.certAlias)){
            X509Certificate[] ret = delegate.getCertificateChain(certAlias);
            LOG.info("Certificate Chain: {}", Arrays.toString(ret));
            return ret;
        } else {
            LOG.warn("Invalid certAlias '{}' in getCertificateChain() in class X509ExtendedKeyManager", certAlias);
            LOG.warn("Expected: '{}'", this.certAlias);
            return null;
        }
    }

    @Override
    /* returns a privateKey of a given certificateAlias;
    *  called by client and server in TLS Handshake after alias was chosen */
    public PrivateKey getPrivateKey(String certAlias) {
        if (certAlias.equals(this.certAlias)){
            return delegate.getPrivateKey(certAlias);
        } else {
            LOG.warn("Invalid certAlias '{}' in getPrivateKey() in class X509ExtendedKeyManager", certAlias);
            LOG.warn("Expected: '{}'", this.certAlias);
            return null;
        }
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine sslEngine){
        return delegate.chooseEngineClientAlias(keyType, issuers, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine){
        return delegate.chooseEngineServerAlias(keyType, issuers, sslEngine);
    }
}
