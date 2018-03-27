/*-
 * ========================LICENSE_START=================================
 * ACME v2 client
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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

import de.fhg.aisec.ids.api.acme.AcmeClient;
import org.apache.karaf.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component(immediate=true, property = {
        Scheduler.PROPERTY_SCHEDULER_EXPRESSION + "=0 * * * * ?"
})
public class AcmeClientService implements AcmeClient, Runnable {

    private static final String[] DOMAINS = {"localhost"};
    public static final URI ACME_URL = URI.create("acme://boulder");
    public static final FileSystem fs = FileSystems.getDefault();
    private static final Logger LOG = LoggerFactory.getLogger(AcmeClientService.class);
    private static Map<String, String> challengeMap = new HashMap<>();

    public String getChallengeAuthorization(String challenge) {
        return challengeMap.get(challenge);
    }

    public void requestCertificate() {
        try {
            // Start ACME challenge responder
            AcmeChallengeServer.startServer(this);

            Arrays.asList("acme.key", "domain.key").forEach(keyFile -> {
                Path keyFilePath = fs.getPath(keyFile);
                if (!keyFilePath.toFile().exists()) {
                    KeyPair keyPair = KeyPairUtils.createKeyPair(4096);
                    try (Writer fileWriter = Files.newBufferedWriter(keyFilePath, StandardCharsets.UTF_8)) {
                        KeyPairUtils.writeKeyPair(keyPair, fileWriter);
                        LOG.info("Successfully created RSA KeyPair: " + fs.getPath(keyFile).toAbsolutePath());
                    } catch (IOException e) {
                        LOG.error("Could not write key pair", e);
                        throw new RuntimeException(e);
                    }
                }
            });

            KeyPair acmeKeyPair;
            try (Reader fileReader = Files.newBufferedReader(fs.getPath("acme.key"), StandardCharsets.UTF_8)) {
                acmeKeyPair = KeyPairUtils.readKeyPair(fileReader);
            } catch (IOException e) {
                LOG.error("Could not read ACME key pair", e);
                throw new RuntimeException(e);
            }

            Account account;
            try {
                Session session = new Session(ACME_URL);
                LOG.info(session.getMetadata().getTermsOfService().toString());
                account = new AccountBuilder().agreeToTermsOfService().useKeyPair(acmeKeyPair).create(session);
                LOG.info(account.getLocation().toString());
            } catch (AcmeException e) {
                LOG.error("Error while accessing/creating ACME account", e);
                throw new RuntimeException(e);
            }

            Order order;
            try {
                order = account.newOrder().domains(DOMAINS).create();
                order.getAuthorizations().parallelStream().map(authorization ->
                        (Http01Challenge) authorization.findChallenge(Http01Challenge.TYPE)).forEach(challenge -> {
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
                            throw new RuntimeException("Failed to successfully solve challenge");
                        }
                    } catch (AcmeException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (AcmeException e) {
                LOG.error("Error while placing certificate order", e);
                throw new RuntimeException(e);
            }

            try (Reader keyReader = Files.newBufferedReader(fs.getPath("domain.key"), StandardCharsets.UTF_8);
                 Writer csrWriter = Files.newBufferedWriter(fs.getPath("domain.csr"), StandardCharsets.UTF_8);
                 OutputStream jksOutputStream = Files.newOutputStream(fs.getPath("test.jks"));
                 Writer chainWriter = Files.newBufferedWriter(fs.getPath("cert-chain.crt"), StandardCharsets.UTF_8))
            {
                KeyPair domainKeyPair = KeyPairUtils.readKeyPair(keyReader);

                System.out.println(domainKeyPair.getPrivate().getFormat());
                System.out.println(domainKeyPair.getPrivate().getAlgorithm());
                System.out.println(domainKeyPair.getPrivate().getClass());

                CSRBuilder csrb = new CSRBuilder();
                csrb.addDomains(DOMAINS);
                csrb.setOrganization("Fraunhofer ACME Demo");
                csrb.sign(domainKeyPair);
                csrb.write(csrWriter);
                order.execute(csrb.getEncoded());
                Certificate certificate = order.getCertificate();

                try {
                    KeyStore store = KeyStore.getInstance("JKS");
                    store.load(null);
                    store.setKeyEntry("ids", domainKeyPair.getPrivate(), "ids".toCharArray(),
                            certificate.getCertificateChain().toArray(new X509Certificate[0]));
                    store.store(jksOutputStream, "ids".toCharArray());
                } catch (KeyStoreException|NoSuchAlgorithmException|CertificateException e) {
                    LOG.error("Error whilst creating KeyStore!", e);
                }

                // Print and save certificate chain
                System.out.println("------------- BEGIN CERTIFICATE CHAIN -------------");
                certificate.getCertificateChain().forEach(System.out::println);
                certificate.writeCertificate(chainWriter);
                System.out.println("-------------- END CERTIFICATE CHAIN --------------");
            } catch (IOException e) {
                LOG.error("Could not read ACME key pair", e);
                throw new RuntimeException(e);
            } catch (AcmeException e) {
                LOG.error("Error while retrieving certificate", e);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            LOG.error("Failed to start HTTP server", e);
            throw new RuntimeException(e);
        } finally {
            // Stop ACME challenge responder
            AcmeChallengeServer.stopServer();
        }
    }

    @Activate
    @Override
    public void run() {
        System.out.println("Upon start, and every minute again...");
    }

}
