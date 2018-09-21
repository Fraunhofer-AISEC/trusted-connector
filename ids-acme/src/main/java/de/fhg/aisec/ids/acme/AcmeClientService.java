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
package de.fhg.aisec.ids.acme;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.api.acme.AcmeTermsOfService;
import de.fhg.aisec.ids.api.acme.SslContextFactoryReloadable;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.karaf.scheduler.Scheduler;
import org.osgi.service.component.annotations.*;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
  immediate = true,
  property = {
    Scheduler.PROPERTY_SCHEDULER_EXPRESSION + "=0 0 3 * * ?" // Every day at 3:00 (3 am)
  }
)
public class AcmeClientService implements AcmeClient, Runnable {

  public static final double RENEWAL_THRESHOLD = 100. / 3.;
  public static final String KEYSTORE_LATEST = "keystore_latest.jks";
  private static final Logger LOG = LoggerFactory.getLogger(AcmeClientService.class);
  private static Map<String, String> challengeMap = new HashMap<>();

  /*
   * The following block subscribes this component to the Settings Service
   */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  private Settings settings = null;

  private Set<SslContextFactoryReloadable> sslReloadables =
      Collections.synchronizedSet(new HashSet<>());
  /*
   * The following block subscribes this component to any SslContextFactoryReloader.
   *
   * A SslContextFactoryReloader is expected to refresh all TLS connections with new
   * certificates from the key store.
   */
  @Reference(
    name = "dynamic-tls-reload-service",
    service = SslContextFactoryReloadable.class,
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    unbind = "unbindSslContextFactoryReloadable"
  )
  protected void bindSslContextFactoryReloadable(SslContextFactoryReloadable reloadable) {
    LOG.info("Bound SslContextFactoryReloadable in AcmeClientService");
    this.sslReloadables.add(reloadable);
  }

  @SuppressWarnings("unused")
  protected void unbindSslContextFactoryReloadable(SslContextFactoryReloadable factory) {
    this.sslReloadables.remove(factory);
  }

  @Override
  public AcmeTermsOfService getTermsOfService(URI acmeServerUri) {
    try {
      Session session = new Session(acmeServerUri);
      URI tosUri = session.getMetadata().getTermsOfService();
      try (InputStream tosStream = tosUri.toURL().openStream()) {
        String tos = CharStreams.toString(new InputStreamReader(tosStream, Charsets.UTF_8));
        return new AcmeTermsOfService(tos, false, null);
      } catch (IOException ioe) {
        return new AcmeTermsOfService(tosUri.toString(), true, null);
      }
    } catch (Exception e) {
      LOG.error("ACME ToS retrieval error", e);
      return new AcmeTermsOfService(
          null, false, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  @Override
  public String getChallengeAuthorization(String challenge) {
    return challengeMap.get(challenge);
  }

  @Override
  public void renewCertificate(
      Path targetDirectory, URI acmeServerUri, String[] domains, int challengePort) {
    try {
      Arrays.asList("acme.key", "domain.key")
          .forEach(
              keyFile -> {
                Path keyFilePath = targetDirectory.resolve(keyFile);
                if (!keyFilePath.toFile().exists()) {
                  KeyPair keyPair = KeyPairUtils.createKeyPair(4096);
                  try (Writer fileWriter =
                      Files.newBufferedWriter(keyFilePath, StandardCharsets.UTF_8)) {
                    KeyPairUtils.writeKeyPair(keyPair, fileWriter);
                    LOG.info(
                        "Successfully created RSA KeyPair: {}",
                        targetDirectory.resolve(keyFile).toAbsolutePath());
                  } catch (IOException e) {
                    LOG.error("Could not write key pair", e);
                    throw new AcmeClientException(e);
                  }
                }
              });

      KeyPair acmeKeyPair;
      try (Reader fileReader =
          Files.newBufferedReader(targetDirectory.resolve("acme.key"), StandardCharsets.UTF_8)) {
        acmeKeyPair = KeyPairUtils.readKeyPair(fileReader);
      } catch (IOException e) {
        LOG.error("Could not read ACME key pair", e);
        throw new AcmeClientException(e);
      }

      Account account;
      // It may happen that certain ACME protocol implementations (provided as SPI services) are not
      // ready yet.
      // This situation leads to an IllegalArgumentException.
      // We will retry up to 3 times until operation is completed successfully or another Exception
      // is thrown.
      int sessionTries = 0;
      while (true) {
        try {
          Session session = new Session(acmeServerUri);
          account =
              new AccountBuilder().agreeToTermsOfService().useKeyPair(acmeKeyPair).create(session);
          if (LOG.isInfoEnabled()) {
            LOG.info(account.getLocation().toString());
          }
          break;
        } catch (AcmeException e) {
          // In case of ACME error, session creation has failed; return immediately.
          LOG.error("Error while accessing/creating ACME account", e);
          return;
        } catch (IllegalArgumentException iae) {
          if (sessionTries++ == 3) {
            LOG.error("Got IllegalArgumentException 3 times, leaving renewal task...");
            return;
          } else {
            LOG.error(
                "Got an IllegalArgumentException, maybe the ACME protocol handler "
                    + "is not available yet. Retry in 10 seconds...",
                iae);
            // Wait 10 seconds before trying again
            try {
              Thread.sleep(10000);
            } catch (InterruptedException ie) {
              LOG.error("Interrupt error during 10-seconds-delay", ie);
              Thread.currentThread().interrupt();
            }
          }
        }
      }

      // Start ACME challenge responder
      AcmeChallengeServer.startServer(this, challengePort);

      Order order;
      try {
        order = account.newOrder().domains(domains).create();
        order
            .getAuthorizations()
            .parallelStream()
            .map(
                authorization ->
                    (Http01Challenge) authorization.findChallenge(Http01Challenge.TYPE))
            .forEach(
                challenge -> {
                  challengeMap.put(challenge.getToken(), challenge.getAuthorization());
                  try {
                    // solve the challenge
                    challenge.trigger();
                    do {
                      try {
                        Thread.sleep(1000L);
                      } catch (InterruptedException ie) {
                        LOG.error("Error while doing 1 second sleep");
                        Thread.currentThread().interrupt();
                      }
                      challenge.update();
                      LOG.info(challenge.getStatus().toString());
                    } while (challenge.getStatus() == Status.PENDING);
                    if (challenge.getStatus() != Status.VALID) {
                      throw new AcmeClientException("Failed to successfully solve challenge");
                    }
                  } catch (AcmeException e) {
                    throw new AcmeClientException(e);
                  }
                });
      } catch (AcmeException e) {
        LOG.error("Error while placing certificate order", e);
        throw new AcmeClientException(e);
      }

      String timestamp =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"));
      try (Reader keyReader =
              Files.newBufferedReader(
                  targetDirectory.resolve("domain.key"), StandardCharsets.UTF_8);
          Writer csrWriter =
              Files.newBufferedWriter(
                  targetDirectory.resolve("csr_ " + timestamp + ".csr"), StandardCharsets.UTF_8);
          Writer chainWriter =
              Files.newBufferedWriter(
                  targetDirectory.resolve("cert-chain_" + timestamp + ".crt"),
                  StandardCharsets.UTF_8)) {
        KeyPair domainKeyPair = KeyPairUtils.readKeyPair(keyReader);

        CSRBuilder csrb = new CSRBuilder();
        csrb.addDomains(domains);
        // TODO: Retrieve such information from settings/info-model
        csrb.setOrganization("Trusted Connector");
        csrb.sign(domainKeyPair);
        csrb.write(csrWriter);
        order.execute(csrb.getEncoded());

        // Download and save certificate
        Certificate certificate = order.getCertificate();
        certificate.writeCertificate(chainWriter);
        // Create JKS keystore from key and certificate chain
        Path keyStorePath = targetDirectory.resolve("keystore_" + timestamp + ".jks");
        try (OutputStream jksOutputStream = Files.newOutputStream(keyStorePath)) {
          KeyStore store = KeyStore.getInstance("JKS");
          store.load(null);
          store.setKeyEntry(
              "ids",
              domainKeyPair.getPrivate(),
              "ids".toCharArray(),
              certificate.getCertificateChain().toArray(new X509Certificate[0]));
          store.store(jksOutputStream, "ids".toCharArray());
          // If there is a SslContextFactoryReloader, make it refresh the TLS connections.
          LOG.info(
              "Reloading of {} SslContextFactoryReloadable implementations...",
              sslReloadables.size());
          sslReloadables.forEach(r -> r.reload(keyStorePath.toString()));
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
          LOG.error("Error whilst creating new KeyStore!", e);
        }
        Files.copy(
            keyStorePath,
            targetDirectory.resolve(KEYSTORE_LATEST),
            StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        LOG.error("Could not read ACME key pair", e);
        throw new AcmeClientException(e);
      } catch (AcmeException e) {
        LOG.error("Error while retrieving certificate", e);
        throw new AcmeClientException(e);
      }
    } catch (IOException e) {
      LOG.error("Failed to start HTTP server", e);
      throw new AcmeClientException(e);
    } finally {
      // Stop ACME challenge responder
      AcmeChallengeServer.stopServer();
    }
  }

  public void renewalCheck(
      Path targetDirectory, String acmeServerUrl, String[] domains, int challengePort) {
    if (acmeServerUrl.isEmpty()) {
      LOG.info("ACME server URL is empty, skipping renewal check.");
      return;
    }
    try (InputStream jksInputStream =
        Files.newInputStream(targetDirectory.resolve(KEYSTORE_LATEST))) {
      KeyStore store = KeyStore.getInstance("JKS");
      store.load(jksInputStream, "ids".toCharArray());
      X509Certificate cert = (X509Certificate) store.getCertificateChain("ids")[0];
      long now = new Date().getTime();
      long notBeforeTime = cert.getNotBefore().getTime();
      long notAfterTime = cert.getNotAfter().getTime();
      double validityPercentile =
          100. * (double) (notAfterTime - now) / (double) (notAfterTime - notBeforeTime);
      if (LOG.isInfoEnabled()) {
        LOG.info(
            String.format(
                "Remaining relative validity span (%s): %.2f%%",
                targetDirectory.toString(), validityPercentile));
      }
      if (validityPercentile < RENEWAL_THRESHOLD) {
        if (LOG.isInfoEnabled()) {
          LOG.info(
              String.format(
                  "%.2f < %.2f, requesting renewal", validityPercentile, RENEWAL_THRESHOLD));
        }
        // Do the renewal in a separate Thread such that other stuff can be executed in parallel.
        // This is especially important if the ACME protocol implementations are missing upon boot.
        Thread t =
            new Thread(
                () ->
                    renewCertificate(
                        targetDirectory, URI.create(acmeServerUrl), domains, challengePort));
        t.setName("ACME Renewal Thread");
        t.setDaemon(true);
        t.start();
      }
    } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
      LOG.error("Error in web console keystore renewal check", e);
    }
  }

  @Activate
  @Override
  public void run() {
    LOG.info("ACME renewal job has been triggered (once upon start and daily at 3:00).");
    try {
      ConnectorConfig config = settings.getConnectorConfig();
      renewalCheck(
          FileSystems.getDefault().getPath("etc", "tls-webconsole"),
          config.getAcmeServerWebcon(),
          config.getAcmeDnsWebcon().trim().split("\\s*,\\s*"),
          config.getAcmePortWebcon());
    } catch (Exception e) {
      LOG.error("ACME Renewal task failed", e);
    }
  }
}
