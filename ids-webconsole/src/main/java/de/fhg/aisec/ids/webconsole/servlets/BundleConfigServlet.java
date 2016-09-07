package de.fhg.aisec.ids.webconsole.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;

/**
 * Servlet for configuring OSGi bundles.
 * 
 * There is only one supported operation:
 * 
 * <servletname>	(GET/POST)	Return a JSON array of informations about all bundles currently installed in the platform.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(service=Servlet.class, property = {"alias=/ids/bundleconfig"})
public class BundleConfigServlet extends HttpServlet {
	private static final long serialVersionUID = -5019172752175841550L;
	private BundleContext ctx = null;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		PrintWriter writer = resp.getWriter();
		writer.write("[\n");
		int index = 0;
		for (Bundle b : ctx.getBundles()) {
			long id = b.getBundleId();
			String name = b.getSymbolicName();
			int state = b.getState();
			String version = b.getVersion().toString();
			
			writer.write("    {\n" + "        \"id\":" + id + ",\n"
					+ "        \"name\": \"" + name + "\",\n"
					+ "        \"version\": \"" + name + "\",\n"
					+ "        \"status\": \"" + state + "\"\n" + "    }");
			if (index < ctx.getBundles().length - 1) {
				writer.write(",\n");
			}
			index++;
		}
		writer.write("]");
		writer.flush();
	}
}
