package de.fhg.aisec.ids.webconsole.api;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Component;

/**
 * This filter adds Cross-Origin Resource Sharing (CORS) headers to each
 * response.
 * 
 * @author Christian Banse
 */
@Provider
@Component(immediate = true)
public class CORSResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        System.out.println("Filtering");
        
        // allow AJAX from everywhere
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, X-Requested-With");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

}
