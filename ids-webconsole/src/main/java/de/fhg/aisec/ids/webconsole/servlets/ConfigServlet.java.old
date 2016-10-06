package de.fhg.aisec.ids.webconsole.servlets;

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

@Component(service=Servlet.class, property = {"alias=/ids/config"})
public class ConfigServlet extends HttpServlet {
	private static final long serialVersionUID = -5081917752175841550L;	
	private static final Logger LOG = LoggerFactory.getLogger(ConfigServlet.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getPathInfo()==null) {
			return;
		}
		
		switch (req.getPathInfo()) {
		case "/getProp":
			getProperty(req, resp);
			break;
		case "/list":
			list(req, resp);
			break;
		default:
			break;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		switch (req.getPathInfo()) {
		case "/setprop":
			LOG.debug("Setting config property " + req);
			setProperty(req, resp);
			break;
		default:
			LOG.warn("Unsupported POST request to ConfigServlet: " + req.getPathInfo());
			break;
		}
	}

	private void setProperty(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		try (PrintWriter writer = resp.getWriter()) {
			req.getParameterMap().keySet()
					.forEach(k -> WebConsoleComponent.getConfigService().set(k, req.getParameter(k)));
			writer.println("OK");
			writer.flush();
		}
	}

	private void getProperty(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		try (PrintWriter writer = resp.getWriter()) {
//			JsonObject json = new JsonObject();
//			req.getParameterMap().keySet()
//					.forEach(k -> json.add(k, (String) WebConsoleComponent.getConfigService().get(k)));
//			writer.println(json.asString());
			writer.flush();
		}
	}

	private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		try (PrintWriter writer = resp.getWriter()) {
//			JsonObject json = new JsonObject();
//			WebConsoleComponent.getConfigService().list().forEach((k, v) -> json.add(k.toString(), v.toString()));
//			writer.println(json.toString());
			writer.flush();
		}
	}
}
