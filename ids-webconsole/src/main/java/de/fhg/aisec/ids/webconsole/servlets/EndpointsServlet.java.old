package de.fhg.aisec.ids.webconsole.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.eclipsesource.json.JsonArray;
//import com.eclipsesource.json.JsonObject;

//import de.fhg.aisec.ids.api.internal.client.CorePlatformClient;
import de.fhg.aisec.ids.messages.BrokerProtos.ServiceDescription;

/**
 * Provides information on the currently provided endpoints of this connector.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(service=Servlet.class, property = {"alias=/ids/endpoints"})
public class EndpointsServlet extends HttpServlet {
	private static final long serialVersionUID = -5081917752175842550L;
	private final static Logger LOG = LoggerFactory.getLogger(EndpointsServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		LOG.debug("Received GET request " + req.getPathInfo());

		if (req.getPathInfo()==null) {
			return;
		}
				
		switch (req.getPathInfo()) {
		case "/list":
			respondEndpointList(req, resp);
			break;
		default:
			LOG.warn("Unknown request");
			break;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

	}

	/**
	 * Returns a list of all endpoints registered at the broker.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void respondEndpointList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
//		CorePlatformClient client = new CorePlatformClient();
//
//		try {
//			if (client.connect().get()) {
//				LOG.info("Getting list of services from broker");
//				CompletableFuture<List<ServiceDescription>> servicesF = client.queryBrokerForServices();
//				List<ServiceDescription> services = servicesF.get();
//				LOG.debug("Received list of services: " + services.size());
//				LOG.debug("Disconnecting from internal API");
//				client.disconnect();
//				JsonArray json = new JsonArray();
//				for (ServiceDescription sd : services) {
//					json.add(new JsonObject().add("epAddress", sd.getURI()).add("description", sd.getDescription()));
//				}
//				LOG.debug("Received list of service as JSON: " + json.toString());
//				writer.write(json.toString());
//				writer.flush();
//			}
//		} catch (Throwable e) {
//			LOG.error(e.getMessage(), e);
//			writer.println("ERR: " + e.getMessage());
//			writer.flush();
//		}
	}
}
