package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;

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

    // server and client aliases are cached in a private context (in entryCacheMap) by the X509ExtendedKeyManager
    // implementation. Therefore, getServerAliases() / getClientAliases() returns only uncached aliases since the
    // update on java 11. As we have to check in chooseClientAliases() and chooseServerAlias() if the alias exists in
    // the keystore and we cannot access the cached aliases without an overwritten X509KeyManagerImpl instance, we will
    // also cache the aliases and its properties in the following HashMap.
    private HashMap<String, CachedAliasValue> cachedAliases = new HashMap<>();

    CustomX509ExtendedKeyManager(String alias, String keyType, final X509ExtendedKeyManager delegate){
        super();
        this.certAlias = alias;
        this.keyType = keyType;
        this.delegate = delegate;
    }

    @Override
    public String[] getClientAliases(String s, Principal[] principals) {
        String[] clientAliases = delegate.getClientAliases(s, principals);
        for (String alias : clientAliases)
            cachedAliases.putIfAbsent(alias, new CachedAliasValue(s, principals));
        return clientAliases;
    }

    @Override
    /* returns an existing certAlias that matches one of the given KeyTypes, or null;
       called only by client in TLS handshake */
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        if (Arrays.asList(keyTypes).contains(this.keyType)){
            if ((cachedAliases.containsKey(this.certAlias) &&
                    cachedAliases.get(this.certAlias).match(keyType, issuers))
                    || Arrays.asList(getServerAliases(keyType, issuers)).contains(this.certAlias)) {
                LOG.debug("CertificateAlias is {}", this.certAlias);
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
        String[] serverAliases = delegate.getServerAliases(s, principals);
        for (String alias : serverAliases)
            cachedAliases.putIfAbsent(alias, new CachedAliasValue(s, principals));
        return serverAliases;
    }

    @Override
    /* returns an existing certAlias that matches the given KeyType, or null;
       called only by server in TLS handshake*/
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (keyType.equals(this.keyType)){
            if ((cachedAliases.containsKey(certAlias) && cachedAliases.get(this.certAlias).match(keyType, issuers))
                    || Arrays.asList(getServerAliases(keyType, issuers)).contains(this.certAlias)) {
                LOG.debug("CertificateAlias is {}", this.certAlias);
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
            LOG.debug("Certificate Chain: {}", Arrays.toString(ret));
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

    private class CachedAliasValue {
        private String keyType;
        private Principal[] issuers;

        CachedAliasValue(String keyType, Principal[] issuers){
            this.keyType = keyType;
            this.issuers = issuers;
        }

        boolean match(String keyType, Principal[] principals){
            return this.keyType.equals(keyType) && (principals == null /* || //FIXME check issuers */);
        }
    }
}
