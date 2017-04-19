package de.fhg.ids.attestation;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public class ProtobufFeature implements Feature {
    
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(ProtobufProvider.class)) {
            context.register(ProtobufProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        }
        return true;
    }
}