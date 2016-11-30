package de.fhg.aisec.ids.webconsole.api;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * This filter adds Cross-Origin Resource Sharing (CORS) headers to each
 * response.
 * 
 * @author Christian Banse
 */
public class CORSResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
        ContainerResponseContext responseContext)
        throws IOException {

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        // allow AJAX from everywhere
        headers.add("Access-Control-Allow-Origin", "*");
    }

}
