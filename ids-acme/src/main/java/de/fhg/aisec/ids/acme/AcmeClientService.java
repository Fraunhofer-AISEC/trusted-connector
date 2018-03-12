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
import org.osgi.service.component.annotations.Component;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component(immediate=true, name="acme-client")
public class AcmeClientService implements AcmeClient {

    public static final String[] DOMAINS = {"localhost"};
    public static final URI ACME_URL = URI.create("acme://pebble");
    public static final FileSystem fs = FileSystems.getDefault();
    private static final Logger LOG = LoggerFactory.getLogger(de.fhg.aisec.ids.acme.AcmeClientService.class);
    private static Map<String, String> challengeMap = new HashMap<>();

    public String getChallengeAuthorization(String challenge) {
        return challengeMap.get(challenge);
    }

    public void requestCertificate() {
        Arrays.asList("acme.key", "domain.key").forEach(keyFile -> {
            if (!Files.exists(fs.getPath(keyFile))) {
                KeyPair keyPair = KeyPairUtils.createKeyPair(4096);
                try (FileWriter fileWriter = new FileWriter(keyFile)) {
                    KeyPairUtils.writeKeyPair(keyPair, fileWriter);
                } catch (IOException e) {
                    LOG.error("Could not write key pair", e);
                    throw new RuntimeException(e);
                }
            }
        });

        KeyPair acmeKeyPair;
        try (FileReader fileReader = new FileReader("acme.key")) {
            acmeKeyPair = KeyPairUtils.readKeyPair(fileReader);
        } catch (IOException e) {
            LOG.error("Could not read ACME key pair", e);
            throw new RuntimeException(e);
        }
        Session session = new Session(ACME_URL, acmeKeyPair);

        Account account;
        try {
            LOG.info(session.getMetadata().getTermsOfService().toString());
            try {
                AccountBuilder accountBuilder = new AccountBuilder();
                account = accountBuilder.onlyExisting().create(session);
            } catch (AcmeException e) {
                AccountBuilder accountBuilder = new AccountBuilder();
                accountBuilder.agreeToTermsOfService();
                account = accountBuilder.create(session);
            }
            LOG.info(account.getLocation().toString());
        } catch (AcmeException e) {
            LOG.error("Error while accessing/creating ACME account", e);
            throw new RuntimeException(e);
        }

        Order order;
        try {
            order = account.newOrder().domains(DOMAINS).create();
//            Order order = account.newOrder().domains(DOMAINS).notAfter(Instant.now().plus(Duration.ofDays(20L)))
//                    .create();
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

        try {
            KeyPair domainKeyPair;
            try (Reader keyReader = new FileReader("domain.key");
                 Writer csrWriter = new FileWriter("domain.csr");
                 Writer chainWriter = new FileWriter("cert-chain.crt")) {
                domainKeyPair = KeyPairUtils.readKeyPair(keyReader);
                CSRBuilder csrb = new CSRBuilder();
                csrb.addDomains(DOMAINS);
                csrb.setOrganization("Fraunhofer ACME Demo");
                csrb.sign(domainKeyPair);
                csrb.write(csrWriter);
                order.execute(csrb.getEncoded());
                Certificate certificate = order.getCertificate();
                // Print and save certificate chain
                System.out.println("---------- CERTIFICATE: ----------");
                System.out.println(certificate.getCertificate());
                System.out.println("------------- CHAIN: -------------");
                certificate.getCertificateChain().forEach(System.out::println);
                // Save certificate
                certificate.writeCertificate(chainWriter);
            } catch (IOException e) {
                LOG.error("Could not read ACME key pair", e);
                throw new RuntimeException(e);
            }
        } catch (AcmeException e) {
            LOG.error("Error while retrieving certificate", e);
            throw new RuntimeException(e);
        }
    }

}
