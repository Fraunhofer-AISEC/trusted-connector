/*-
 * ========================LICENSE_START=================================
 * ids-acme
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.acme

import de.fhg.aisec.ids.api.acme.AcmeClient
import de.fhg.aisec.ids.api.acme.AcmeTermsOfService
import de.fhg.aisec.ids.api.acme.SslContextFactoryReloadable
import de.fhg.aisec.ids.api.settings.Settings
import org.apache.karaf.scheduler.Scheduler
import org.shredzone.acme4j.Account
import org.shredzone.acme4j.AccountBuilder
import org.shredzone.acme4j.Order
import org.shredzone.acme4j.Session
import org.shredzone.acme4j.Status
import org.shredzone.acme4j.challenge.Http01Challenge
import org.shredzone.acme4j.exception.AcmeException
import org.shredzone.acme4j.exception.AcmeNetworkException
import org.shredzone.acme4j.util.CSRBuilder
import org.shredzone.acme4j.util.KeyPairUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Collections
import java.util.Date

@Component(
    "idsAcmeClient"
    // TODO: Scheduling in Spring
    // Every day at 3:00 (3 am)
    // property = [Scheduler.PROPERTY_SCHEDULER_EXPRESSION + "=0 0 3 * * ?"]
)
class AcmeClientService : AcmeClient, Runnable {

    /*
     * The following block subscribes this component to the Settings Service
     */
    @Autowired(required = false)
    private var settings: Settings? = null

    private val sslReloadables = Collections.synchronizedSet(HashSet<SslContextFactoryReloadable>())
    /*
     * The following block subscribes this component to any SslContextFactoryReloader.
     *
     * A SslContextFactoryReloader is expected to refresh all TLS connections with new
     * certificates from the key store.
     */
    // TODO: Adapt for Spring
    // @Reference(
    //     name = "dynamic-tls-reload-service",
    //     service = SslContextFactoryReloadable::class,
    //     cardinality = ReferenceCardinality.MULTIPLE,
    //     unbind = "unbindSslContextFactoryReloadable"
    // )
    private fun bindSslContextFactoryReloadable(reloadable: SslContextFactoryReloadable) {
        LOG.info("Bound SslContextFactoryReloadable in AcmeClientService")
        this.sslReloadables.add(reloadable)
    }

    @Suppress("unused")
    private fun unbindSslContextFactoryReloadable(factory: SslContextFactoryReloadable) {
        this.sslReloadables.remove(factory)
    }

    override fun getTermsOfService(acmeServerUri: URI): AcmeTermsOfService {
        try {
            val session = Session(acmeServerUri)
            val tosUri = session.metadata.termsOfService
            try {
                tosUri!!.toURL().openStream().use { tosStream ->
                    val tos = InputStreamReader(tosStream, StandardCharsets.UTF_8).readText()
                    return AcmeTermsOfService(tos, false, null)
                }
            } catch (ioe: IOException) {
                return AcmeTermsOfService(tosUri!!.toString(), true, null)
            }
        } catch (e: Exception) {
            LOG.error("ACME ToS retrieval error", e)
            return AcmeTermsOfService(null, false, e.javaClass.simpleName + ": " + e.message)
        }
    }

    override fun getChallengeAuthorization(challenge: String): String? {
        return challengeMap[challenge]
    }

    private fun ensureKeys(targetDirectory: Path) {
        Arrays.asList("acme.key", "domain.key").forEach { keyFile ->
            val keyFilePath = targetDirectory.resolve(keyFile)
            if (!keyFilePath.toFile().exists()) {
                val keyPair = KeyPairUtils.createKeyPair(4096)
                try {
                    Files.newBufferedWriter(keyFilePath, StandardCharsets.UTF_8).use { fileWriter ->
                        KeyPairUtils.writeKeyPair(keyPair, fileWriter)
                        LOG.info(
                            "Successfully created RSA KeyPair: {}",
                            targetDirectory.resolve(keyFile).toAbsolutePath()
                        )
                    }
                } catch (e: IOException) {
                    LOG.error("Could not write key pair", e)
                    throw AcmeClientException(e)
                }
            }
        }
    }

    private fun getACMEKeyPair(targetDirectory: Path): KeyPair {
        try {
            Files.newBufferedReader(targetDirectory.resolve("acme.key"), StandardCharsets.UTF_8)
                .use { fileReader ->
                    return KeyPairUtils.readKeyPair(fileReader)
                }
        } catch (e: IOException) {
            LOG.error("Could not read ACME key pair", e)
            throw AcmeClientException(e)
        }
    }

    override fun renewCertificate(
        targetDirectory: Path,
        acmeServerUri: URI,
        domains: Array<String>,
        challengePort: Int
    ) {
        try {
            ensureKeys(targetDirectory)

            var account: Account
            // It may happen that certain ACME protocol implementations
            // (provided as SPI services) are not ready yet.
            // This situation leads to an IllegalArgumentException.
            // We will retry up to 3 times until operation is completed
            // successfully or another Exception is thrown.
            var sessionTries = 0
            while (true) {
                try {
                    val session = Session(acmeServerUri)
                    account =
                        AccountBuilder()
                            .agreeToTermsOfService()
                            .useKeyPair(getACMEKeyPair(targetDirectory))
                            .create(session)
                    if (LOG.isInfoEnabled) {
                        LOG.info(account.location.toString())
                    }
                    break
                } catch (e: AcmeNetworkException) {
                    // In case of ACME error, session creation has failed; return immediately.
                    LOG.warn(
                        "Could not connect to ACME server {}. Will not create certificate.",
                        acmeServerUri
                    )
                    return
                } catch (e: AcmeException) {
                    // In case of ACME error, session creation has failed; return immediately.
                    LOG.error("Error while accessing/creating ACME account", e)
                    return
                } catch (iae: IllegalArgumentException) {
                    if (sessionTries++ == 3) {
                        LOG.error("Got IllegalArgumentException 3 times, leaving renewal task...")
                        return
                    } else {
                        LOG.error(
                            "Got an IllegalArgumentException, maybe the ACME protocol handler " +
                                "is not available yet. Retry in 10 seconds...",
                            iae
                        )
                        // Wait 10 seconds before trying again
                        try {
                            Thread.sleep(10000)
                        } catch (ie: InterruptedException) {
                            LOG.error("Interrupt during 10-seconds-delay", ie)
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }

            // Start ACME challenge responder
            AcmeChallengeServer.startServer(this, challengePort)

            val order: Order
            try {
                order = account.newOrder().domains(*domains).create()
                order
                    .authorizations
                    .parallelStream()
                    .map<Http01Challenge> { authorization ->
                        authorization.findChallenge(Http01Challenge.TYPE)
                    }
                    .forEach { challenge ->
                        challengeMap[challenge.token] = challenge.authorization
                        try {
                            // solve the challenge
                            challenge.trigger()
                            do {
                                try {
                                    Thread.sleep(1000L)
                                } catch (ie: InterruptedException) {
                                    LOG.error("Error while doing 1 second sleep")
                                    Thread.currentThread().interrupt()
                                }

                                challenge.update()
                                LOG.info(challenge.status.toString())
                            } while (challenge.status == Status.PENDING)
                            if (challenge.status != Status.VALID) {
                                throw AcmeClientException("Failed to successfully solve challenge")
                            }
                        } catch (e: AcmeException) {
                            throw AcmeClientException(e)
                        }
                    }
            } catch (e: AcmeException) {
                LOG.error("Error while placing certificate order", e)
                throw AcmeClientException(e)
            }

            val timestamp =
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"))
            try {
                Files.newBufferedReader(
                    targetDirectory.resolve("domain.key"),
                    StandardCharsets.UTF_8
                )
                    .use { keyReader ->
                        Files.newBufferedWriter(
                            targetDirectory.resolve("csr_ $timestamp.csr"),
                            StandardCharsets.UTF_8
                        )
                            .use { csrWriter ->
                                Files.newBufferedWriter(
                                    targetDirectory.resolve("cert-chain_$timestamp.crt"),
                                    StandardCharsets.UTF_8
                                )
                                    .use { chainWriter ->
                                        val domainKeyPair = KeyPairUtils.readKeyPair(keyReader)

                                        val csrb = CSRBuilder()
                                        csrb.addDomains(*domains)
                                        // TODO: Retrieve such information from settings/info-model
                                        csrb.setOrganization("Trusted Connector")
                                        csrb.sign(domainKeyPair)
                                        csrb.write(csrWriter)
                                        order.execute(csrb.encoded)

                                        // Download and save certificate
                                        val certificate = order.certificate
                                        certificate!!.writeCertificate(chainWriter)
                                        // Create PKCS12 keystore from key and certificate chain
                                        val keyStorePath =
                                            targetDirectory.resolve("keystore_$timestamp.p12")
                                        try {
                                            Files.newOutputStream(keyStorePath).use { ksOutputStream
                                                ->
                                                val store = KeyStore.getInstance("PKCS12")
                                                store.load(null)
                                                store.setKeyEntry(
                                                    "ids",
                                                    domainKeyPair.private,
                                                    "password".toCharArray(),
                                                    certificate.certificateChain.toTypedArray<
                                                        X509Certificate>()
                                                )
                                                store.store(
                                                    ksOutputStream,
                                                    "password".toCharArray()
                                                )
                                                // If there is a SslContextFactoryReloader, make it
                                                // refresh the TLS connections.
                                                LOG.info(
                                                    "Reloading of {} SslContextFactoryReloadable implementations...",
                                                    sslReloadables.size
                                                )
                                                sslReloadables.forEach { r ->
                                                    r.reload(keyStorePath.toString())
                                                }
                                            }
                                        } catch (e: Exception) {
                                            LOG.error("Error whilst creating new KeyStore!", e)
                                        }

                                        Files.copy(
                                            keyStorePath,
                                            targetDirectory.resolve(KEYSTORE_LATEST),
                                            StandardCopyOption.REPLACE_EXISTING
                                        )
                                    }
                            }
                    }
            } catch (e: IOException) {
                LOG.error("Could not read ACME key pair", e)
                throw AcmeClientException(e)
            } catch (e: AcmeException) {
                LOG.error("Error while retrieving certificate", e)
                throw AcmeClientException(e)
            }
        } catch (e: IOException) {
            LOG.error("Failed to start HTTP server", e)
            throw AcmeClientException(e)
        } finally {
            // Stop ACME challenge responder
            AcmeChallengeServer.stopServer()
        }
    }

    fun renewalCheck(
        targetDirectory: Path,
        acmeServerUrl: String,
        domains: Array<String>,
        challengePort: Int
    ) {
        if (acmeServerUrl.isEmpty()) {
            LOG.info("ACME server URL is empty, skipping renewal check.")
            return
        }
        try {
            Files.newInputStream(targetDirectory.resolve(KEYSTORE_LATEST)).use { ksInputStream ->
                val store = KeyStore.getInstance("PKCS12")
                store.load(ksInputStream, "password".toCharArray())
                val cert = store.getCertificateChain("ids")[0] as X509Certificate
                val now = Date().time
                val notBeforeTime = cert.notBefore.time
                val notAfterTime = cert.notAfter.time
                val validityPercentile =
                    100.0 * (notAfterTime - now).toDouble() /
                        (notAfterTime - notBeforeTime).toDouble()
                if (LOG.isInfoEnabled) {
                    LOG.info(
                        String.format(
                            "Remaining relative validity span (%s): %.2f%%",
                            targetDirectory.toString(),
                            validityPercentile
                        )
                    )
                }
                if (validityPercentile < RENEWAL_THRESHOLD) {
                    if (LOG.isInfoEnabled) {
                        LOG.info(
                            String.format(
                                "%.2f < %.2f, requesting renewal",
                                validityPercentile,
                                RENEWAL_THRESHOLD
                            )
                        )
                    }
                    // Do the renewal in a separate Thread such that other stuff can be executed in
                    // parallel.
                    // This is especially important if the ACME protocol implementations are missing
                    // upon boot.
                    val t = Thread {
                        renewCertificate(
                            targetDirectory,
                            URI.create(acmeServerUrl),
                            domains,
                            challengePort
                        )
                    }
                    t.name = "ACME Renewal Thread"
                    t.isDaemon = true
                    t.start()
                }
            }
        } catch (e: Exception) {
            LOG.error("Error in web console keystore renewal check", e)
        }
    }

    @Activate
    override fun run() {
        LOG.info("ACME renewal job has been triggered (once upon start and daily at 3:00).")
        try {
            val config = settings!!.connectorConfig
            renewalCheck(
                FileSystems.getDefault().getPath("etc", "tls-webconsole"),
                config.acmeServerWebcon,
                config
                    .acmeDnsWebcon
                    .trim { it <= ' ' }
                    .split("\\s*,\\s*".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray(),
                config.acmePortWebcon
            )
        } catch (e: Exception) {
            LOG.error("ACME Renewal task failed", e)
        }
    }

    companion object {

        const val RENEWAL_THRESHOLD = 100.0 / 3.0
        const val KEYSTORE_LATEST = "keystore_latest.p12"
        private val LOG = LoggerFactory.getLogger(AcmeClientService::class.java)!!
        private val challengeMap = HashMap<String, String>()
    }
}
