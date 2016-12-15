package de.fhg.ids.attestation;
import javax.ws.rs.core.MediaType;

public class MediaTypeExt extends MediaType {
    /**
     * A {@code String} constant representing "{@value #APPLICATION_PROTOBUF}" media type.
     */
    public final static String APPLICATION_PROTOBUF = "application/x-protobuf";
    /**
     * A {@link javax.ws.rs.core.MediaType} constant representing "{@value #APPLICATION_PROTOBUF}" media type.
     */
    public final static MediaType APPLICATION_PROTOBUF_TYPE = new MediaType("application", "x-protobuf");
}