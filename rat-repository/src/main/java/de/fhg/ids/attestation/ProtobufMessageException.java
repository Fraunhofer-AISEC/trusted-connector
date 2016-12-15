package de.fhg.ids.attestation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ProtobufMessageException extends WebApplicationException {
    public ProtobufMessageException(Throwable cause) {
        super(cause);
    }

    @Override
    public Response getResponse() {
        return Response.status(Response.Status.BAD_REQUEST).entity(getCause().getMessage()).type("text/plain").build();
    }
}