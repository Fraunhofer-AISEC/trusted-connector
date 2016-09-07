package de.fhg.aisec.ids.webconsole.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API for handling OSGi bundles in the platform.
 * 
 * Supported commands:
 * 
 * bundles/list Returns a JSON array of bundles. <br>
 * bundles/star?bid=<long> Starts bundle with id <code>bid</code>. <br>
 * bundles/stop?bid=<long> Stops bundle with id <code>bid</code>. <br>
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(service=Servlet.class, property = {"alias=/ids/bundles"})
public class BundlesServlet extends HttpServlet {
	private static final long serialVersionUID = -5081917752175841250L;
	private static final Logger LOG = LoggerFactory.getLogger(BundlesServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		ServletContext sctx = req.getSession().getServletContext();
		BundleContext bctx = (BundleContext) sctx.getAttribute("osgi-bundlecontext");
		
		if (req.getPathInfo()==null) {
			return;
		}
		
		LOG.debug("Received GET request " + req.getPathInfo());
		switch (req.getPathInfo()) {
		case "/list":
			respondBundleList(req, resp, bctx);
			break;
		default:
			break;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received POST request " + req.getPathInfo());
			for (String k : req.getParameterMap().keySet()) {
				for (String v : req.getParameterMap().get(k)) {
					LOG.debug("  " + v);
				}
			}
		}

		ServletContext sctx = req.getSession().getServletContext();
		BundleContext bctx = (BundleContext) sctx.getAttribute("osgi-bundlecontext");

		switch (req.getPathInfo()) {
		case "/start":
			startBundle(req, resp, bctx);
			break;
		case "/stop":
			stopBundle(req, resp, bctx);
			break;
		default:
			break;
		}
	}

	/**
	 * Starts a bundle whose bundle id is give in HTTP parameter
	 * <code>bid</code>.
	 * 
	 * @param req
	 * @param resp
	 * @param bctx 
	 * @throws IOException
	 */
	private void startBundle(HttpServletRequest req, HttpServletResponse resp, BundleContext bctx) throws IOException {
		try (PrintWriter writer = resp.getWriter()) {
			long bid = Long.parseLong(req.getParameter("bid"));
			bctx.getBundle(bid).start();
			writer.println("OK");
			writer.flush();
		} catch (BundleException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Stops a bundle whose bundle id is give in HTTP parameter <code>bid</code>
	 * .
	 * 
	 * @param req
	 * @param resp
	 * @param bctx 
	 * @throws IOException
	 */
	private void stopBundle(HttpServletRequest req, HttpServletResponse resp, BundleContext bctx) throws IOException {
		try (PrintWriter writer = resp.getWriter()) {
			long bid = Long.parseLong(req.getParameter("bid"));
			bctx.getBundle(bid).stop();
			writer.println("OK");
		} catch (BundleException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Returns a list of all bundles in a JSON array.
	 * 
	 * @param req
	 * @param resp
	 * @param bctx 
	 * @throws IOException
	 */
	private void respondBundleList(HttpServletRequest req, HttpServletResponse resp, BundleContext bctx) {
		PrintWriter writer;
		try {
			writer = resp.getWriter();
			writer.write("[\n");
			int index = 0;
			for (Bundle b : bctx.getBundles()) {
				long id = b.getBundleId();
				String name = b.getSymbolicName();
				String state = humanReadableState(b.getState());
				String version = b.getVersion().toString();
				writer.write("    {\n" + "        \"id\":" + id + ",\n" + "        \"name\": \"" + name + "\",\n"
						+ "        \"version\": \"" + version + "\",\n" + "        \"status\": \"" + state + "\"\n"
						+ "    }");
				if (index < bctx.getBundles().length - 1) {
					writer.write(",\n");
				}
				index++;
			}
			writer.write("]");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a human-readable description of the numeric state of an OSGi bundle.
	 * 
	 * @param state
	 * @return
	 */
	private String humanReadableState(int state) {
		switch (state) {
		case Bundle.STARTING: return "STARTING";
		case Bundle.STOPPING: return "STOPPING";
		case Bundle.UNINSTALLED: return "UNINSTALLED";
		case Bundle.RESOLVED: return "RESOLVED";
		case Bundle.INSTALLED: return "INSTALLED";
		case Bundle.ACTIVE: return "ACTIVE";
		}
		return "UNKNOWN";
	}

}
