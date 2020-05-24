/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.aisec.ids.camel.idscp2.server;

import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent;
import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@UriEndpoint(
    scheme = "idscp2server",
    title = "IDSCP2 Server Socket",
    syntax = "idscp2server://host:port",
    label = "ids"
)
public class Idscp2ServerEndpoint extends DefaultEndpoint implements Idscp2EndpointListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ServerEndpoint.class);
    private static final Pattern URI_REGEX = Pattern.compile("(.*?):(\\d+)$");

    private final Idscp2Configuration serverConfiguration;
    private final Idscp2Settings serverSettings;
    private Idscp2Server server;
    private final Set<Idscp2ServerConsumer> consumers = new HashSet<>();

    public Idscp2ServerEndpoint(String uri, String remaining, Idscp2ServerComponent component) {
        super(uri, component);
        final var settings = Idscp2OsgiComponent.getSettings();

        final var remainingMatcher = URI_REGEX.matcher(remaining);
        if (!remainingMatcher.matches()) {
            throw new IllegalArgumentException(remaining + " is not a valid URI remainder, must be \"host:port\".");
        }
        final var matchResult = remainingMatcher.toMatchResult();
        final var host = matchResult.group(1);
        final var port = Integer.parseInt(matchResult.group(2));

        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();

        serverSettings = new Idscp2Settings.Builder()
            .setHost(host)
            .setServerPort(port)
            .setKeyStorePath("etc/idscp2/aisecconnector1-keystore.jks")
            .setTrustStorePath("etc/idscp2/client-truststore_new.jks")
            .setCertificateAlias("1.0.1")
            .setDapsKeyAlias("1")
            .setRatTimeoutDelay(300)
            .build();

        if(settings == null) {
            throw new RuntimeException("Settings not available");
        }

        DefaultDapsDriverConfig config =
            new DefaultDapsDriverConfig.Builder()
                .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
                .setKeyStorePath(serverSettings.getKeyStorePath())
                .setTrustStorePath(serverSettings.getTrustStorePath())
                .setKeyStorePassword(serverSettings.getKeyStorePassword())
                .setTrustStorePassword(serverSettings.getTrustStorePassword())
                .setKeyAlias(serverSettings.getDapsKeyAlias())
                .setDapsUrl(settings.getConnectorConfig().getDapsUrl())
                .build();
        DapsDriver dapsDriver = new DefaultDapsDriver(config);

        RatProverDriverRegistry.getInstance().registerDriver(
            "Dummy", RatProverDummy.class, null);
        RatVerifierDriverRegistry.getInstance().registerDriver(
            "Dummy", RatVerifierDummy.class, null);
        RatProverDriverRegistry.getInstance().registerDriver(
            "TPM2d", TPM2dProver.class,
            new TPM2dProverConfig.Builder().build()
        );
        RatVerifierDriverRegistry.getInstance().registerDriver(
            "TPM2d", TPM2dVerifier.class,
            new TPM2dVerifierConfig.Builder().build()
        );

        serverConfiguration = new Idscp2Configuration(
            this,
            dapsDriver,
            secureChannelDriver,
            serverSettings.getExpectedAttestation(),
            serverSettings.getSupportedAttestation(),
            serverSettings.getRatTimeoutDelay()
        );
    }

    public synchronized void addConsumer(Idscp2ServerConsumer consumer) {
        this.consumers.add(consumer);
        this.server.getAllConnections().forEach(c -> c.addGenericMessageListener(consumer));
    }

    public synchronized void removeConsumer(Idscp2ServerConsumer consumer) {
        this.consumers.remove(consumer);
        this.server.getAllConnections().forEach(c -> c.removeGenericMessageListener(consumer));
    }

    public synchronized void sendMessage(String type, byte[] body) {
        this.server.getAllConnections().forEach(connection -> connection.send(type, body));
    }

    @Override
    public synchronized Producer createProducer() {
        return new Idscp2ServerProducer(this);
    }

    @Override
    public synchronized Consumer createConsumer(Processor processor) {
        return new Idscp2ServerConsumer(this, processor);
    }

    @Override
    public synchronized void onConnection(Idscp2Connection connection) {
        LOG.debug("New IDSCP2 connection on " + getEndpointUri() + ", register consumer listeners");
        this.consumers.forEach(connection::addGenericMessageListener);
    }

    @Override
    public void onError(String error) {
        LOG.error("Error in IDSCP2 server endpoint " + getEndpointUri() + ":\n" + error);
    }

    @Override
    public synchronized void doStart() throws Idscp2Exception {
        LOG.debug("Starting IDSCP2 server endpoint " + getEndpointUri());
        this.server = serverConfiguration.listen(serverSettings);
        ((Idscp2ServerComponent) this.getComponent()).addServer(this.serverSettings.getServerPort(), this.server);
    }

    @Override
    public synchronized void doStop() {
        LOG.debug("Stopping IDSCP2 server endpoint " + getEndpointUri());
        this.server.terminate();
        ((Idscp2ServerComponent) this.getComponent()).removeServer(this.serverSettings.getServerPort());
    }

}
