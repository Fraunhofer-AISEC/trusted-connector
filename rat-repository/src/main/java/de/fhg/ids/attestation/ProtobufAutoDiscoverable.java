package de.fhg.ids.attestation;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.FeatureContext;

@Priority(Priorities.ENTITY_CODER)
public class ProtobufAutoDiscoverable implements AutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        if (!context.getConfiguration().isRegistered(ProtobufFeature.class)) {
            context.register(ProtobufFeature.class);
        }
    }
}