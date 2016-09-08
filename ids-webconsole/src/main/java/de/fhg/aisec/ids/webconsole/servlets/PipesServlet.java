package de.fhg.aisec.ids.webconsole.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.eclipsesource.json.JsonObject;

import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * Webconsole servlet for managing pipes (aka "routes") in the Core Platform. This is basically a web REST interface for the RouteManager OSGi interface.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(service=Servlet.class, property = {"alias=/ids/pipes"})
public class PipesServlet extends HttpServlet {
	private static final long serialVersionUID = -4022599102967147133L;
	private final static Logger LOG = LoggerFactory.getLogger(PipesServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getPathInfo()==null) {
			return;
		}
		
		switch (req.getPathInfo()) {
		case "/list":
			getPipes(req, resp);
			break;
		default:
			break;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		switch (req.getPathInfo()) {
		case "/load":
			loadPipes(req, resp);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Load a new route configuration into the RouteManager.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void loadPipes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
	    try (BufferedReader reader = req.getReader()) {
		    StringBuffer sb = new StringBuffer();
		    String line = "";
		    while ((line = reader.readLine()) != null) {
		      sb.append(line);
		      sb.append("\n");
		    }
		    String config = sb.toString();
		    LOG.debug("Loading new routes config: " + config);
			try (PrintWriter writer = resp.getWriter()) {
				WebConsoleComponent.getRouteManagerService().loadRoutes(config);
				writer.println("OK");
				writer.flush();
			}
	    }
	}
	
	private void getPipes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		try (PrintWriter writer = resp.getWriter()) {
//			JsonObject json = new JsonObject();
//			json.add("pipes", WebConsoleComponent.getRouteManagerService().getRouteConfigAsString());
//			writer.println(json.asObject());
			writer.flush();
		}
	}

}
