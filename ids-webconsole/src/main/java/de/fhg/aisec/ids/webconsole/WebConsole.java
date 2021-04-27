package de.fhg.aisec.ids.webconsole;

import de.fhg.aisec.ids.webconsole.api.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@Component
// Path for legacy reasons
@ApplicationPath("cxf/api/v1")
public class WebConsole extends ResourceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(WebConsole.class);

  public WebConsole() {
    registerEndpoints();
  }

  private void registerEndpoints() {
    LOG.info("Registering WebConsole classes");

    register(AppApi.class);
    register(CertApi.class);
    register(ConfigApi.class);
    register(ConnectionAPI.class);
    register(MetricAPI.class);
    register(PolicyApi.class);
    register(RouteApi.class);
    register(SettingsApi.class);
    register(UserApi.class);

    register(CORSResponseFilter.class);
    register(JWTRestAPIFilter.class);
  }
}
