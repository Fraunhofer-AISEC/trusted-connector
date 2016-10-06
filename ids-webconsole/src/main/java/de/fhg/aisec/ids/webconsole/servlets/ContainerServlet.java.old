package de.fhg.aisec.ids.webconsole.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//
//import com.eclipsesource.json.JsonArray;
//import com.eclipsesource.json.JsonObject;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * Provides information on the currently provided endpoints of this connector.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(service=Servlet.class, property = {"alias=/ids/containers"})
public class ContainerServlet extends HttpServlet {
	private static final long serialVersionUID = -502197752175841550L;
	private final static Logger LOG = LoggerFactory.getLogger(ContainerServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getPathInfo()==null) {
			return;
		}
		
		LOG.debug("Received GET request " + req.getPathInfo());

		String[] parts = req.getPathInfo().replaceFirst("^/", "").split("/");
		String action = (parts.length>1)?parts[1]:parts[0];
		switch (action) {
		case "list":
			respondContainerList(req, resp);
			break;
		case "stop":
			stopContainer(parts[0],resp);
			break;
		case "start":
			startContainer(parts[0],resp);
			break;
		case "wipe":
			wipeContainer(parts[0],resp);
			break;
		default:
			LOG.warn("Unknown request");
			break;
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getPathInfo()==null) {
			return;
		}

		LOG.debug("Received POST request " + req.getPathInfo());
		
		String[] parts = req.getPathInfo().replaceFirst("^/", "").split("/");
		String action = (parts.length>1)?parts[1]:parts[0];
		switch (action) {
		case "pull":
			pullContainer(req,resp);
			break;
		default:
			LOG.warn("Unknown request");
			break;
		}

	}


	private void pullContainer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		String line;
		if ((line = req.getReader().readLine())!=null) {
			line = URLDecoder.decode(line, "UTF-8");
			String[] kv = line.split("=");
			if (kv.length==2 && kv[0].equals("image.id")) {
				PrintWriter writer = resp.getWriter();
				ContainerManager cml = WebConsoleComponent.getContainerManager();
				cml.pullImage(kv[1]);
				writer.println("ok");
				writer.flush();
			}
		}
	}

	private void wipeContainer(String containerId, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.wipe(containerId);
		writer.println("ok");
		writer.flush();
	}

	private void startContainer(String containerId, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.startContainer(containerId);
		writer.println("ok");
		writer.flush();
	}

	private void stopContainer(String containerId, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.stopContainer(containerId);
		writer.println("ok");
		writer.flush();
	}

	/**
	 * Returns a list of all endpoints registered at the broker.
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void respondContainerList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		ContainerManager cml = WebConsoleComponent.getContainerManager();

		try {
			boolean includeStopped = req.getParameter("all") != null && req.getParameter("all").equalsIgnoreCase("true");			
			List<ApplicationContainer> appContainers = cml.list(!includeStopped);			
//			JsonArray json = new JsonArray();
//			for (ApplicationContainer c : appContainers) {
//				json.add(new JsonObject().add("id", c.getId()).add("created", c.getCreated()).add("name", c.getNames()).add("status", c.getStatus()).add("image", c.getImage()).add("size", c.getSize()).add("uptime", c.getUptime()));
//			}
//			LOG.debug("Received list of containers as JSON: " + json.toString());
//			writer.write(json.toString());
			writer.flush();
	
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
			writer.println("ERR: " + e.getMessage());
			writer.flush();
		}
	}
}
