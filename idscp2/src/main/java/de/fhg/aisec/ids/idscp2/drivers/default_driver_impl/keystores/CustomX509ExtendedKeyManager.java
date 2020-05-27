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
public class CustomX509ExtendedKeyManager extends X509ExtendedKeyManager {
    private static final Logger LOG = LoggerFactory.getLogger(CustomX509ExtendedKeyManager.class);

    private final String certAlias;
    private final String keyType;
    private final X509ExtendedKeyManager delegate;

    // server and client aliases are cached in a private context (in entryCacheMap) by the X509ExtendedKeyManager
    // implementation. Therefore, getServerAliases() / getClientAliases() returns only uncached aliases since the
    // update on java 11. As we have to check in chooseClientAliases() and chooseServerAlias() if the alias exists in
    // the keystore and we cannot access the cached aliases without an overwritten X509KeyManagerImpl instance, we will
    // also cache the aliases and its properties in the following HashMap.
    private final HashMap<String, CachedAliasValue> cachedAliases = new HashMap<>();

    CustomX509ExtendedKeyManager(String alias, String keyType, final X509ExtendedKeyManager delegate) {
        super();
        this.certAlias = alias;
        this.keyType = keyType;
        this.delegate = delegate;
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        String[] clientAliases = delegate.getClientAliases(keyType, issuers);
        for (String alias : clientAliases)
            cachedAliases.putIfAbsent(alias, new CachedAliasValue(keyType, null)); //toDo get issuer
        return clientAliases;
    }

    @Override
    /* returns an existing certAlias that matches one of the given KeyTypes, or null;
       called only by client in TLS handshake */
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        if (Arrays.asList(keyTypes).contains(this.keyType)) {
            if ((cachedAliases.containsKey(this.certAlias) &&
                    cachedAliases.get(this.certAlias).match(keyType, issuers))
                    || Arrays.asList(getClientAliases(keyType, issuers)).contains(this.certAlias)) {
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
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        String[] serverAliases = delegate.getServerAliases(keyType, issuers);
        for (String alias : serverAliases)
            cachedAliases.putIfAbsent(alias, new CachedAliasValue(keyType, null)); //toDo get issuer
        return serverAliases;
    }

    @Override
    /* returns an existing certAlias that matches the given KeyType, or null;
       called only by server in TLS handshake*/
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (keyType.equals(this.keyType)) {
            if ((cachedAliases.containsKey(certAlias) && cachedAliases.get(this.certAlias).match(keyType, issuers))
                    || Arrays.asList(getServerAliases(keyType, issuers)).contains(this.certAlias)) {
                LOG.debug("CertificateAlias is {}", this.certAlias);
                return this.certAlias;
            } else {
                LOG.warn("certAlias '{}' was not found in keystore", this.certAlias);
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Different keyType '{}' in chooseServerAlias() in CustomX509ExtendedKeyManager, expected '{}'",
                    keyType, this.keyType);
        }
        return null;
    }

    @Override
    /* returns the certificate chain of a given certificateAlias;
       called by client and server in TLS Handshake after alias was chosen */
    public X509Certificate[] getCertificateChain(String certAlias) {
        if (certAlias.equals(this.certAlias)) {
            return delegate.getCertificateChain(certAlias);
        } else {
            LOG.warn("Different certAlias '{}' in getCertificateChain() in class X509ExtendedKeyManager, " +
                    "expected: '{}'", certAlias, this.certAlias);
            return null;
        }
    }

    @Override
    /* returns a privateKey of a given certificateAlias;
     *  called by client and server in TLS Handshake after alias was chosen */
    public PrivateKey getPrivateKey(String certAlias) {
        if (certAlias.equals(this.certAlias)) {
            return delegate.getPrivateKey(certAlias);
        } else {
            LOG.warn("Different certAlias '{}' in getPrivateKey() in class X509ExtendedKeyManager, expected '{}'",
                    certAlias, this.certAlias);
            return null;
        }
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine sslEngine) {
        return delegate.chooseEngineClientAlias(keyType, issuers, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine) {
        return delegate.chooseEngineServerAlias(keyType, issuers, sslEngine);
    }


    private static class CachedAliasValue {
        private final String keyType; //key algorithm type name
        private final Principal issuer; //certificate issuer

        CachedAliasValue(String keyType, Principal issuer) {
            this.keyType = keyType;
            this.issuer = issuer;
        }

        /*
         * This method is called for a given certificate alias and checks if the corresponding
         * cached alias entry (contains keytype for certAlias and issuer of thee certificate)
         * matches the requested conditions in thee TLS handshake.
         *
         * It must enforce checking if the certAlias belongs to a valid key algorithm type name,
         * e.g. 'RSA' or 'EC' and it must check if the certificate issuer is one of the accepted
         * issuer from the given principals list.
         *
         * returns true, if the keyAlias fulfills the requirements
         */
        boolean match(String keyType, Principal[] issuers) {
            return this.keyType.equals(keyType) && (issuers == null
                    || Arrays.asList(issuers).contains(issuer));
        }
    }
}
