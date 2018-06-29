package de.fhg.aisec.ids.webconsole.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;

public class PortainerCompatibilityIT {

	/**
	 * Retrieves portainer templates as JSON and tries to map it to java using Jackson objectmapper.
	 * 
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		String url = "https://raw.githubusercontent.com/portainer/templates/master/templates.json";
		URL u = new URL(url);
	    HttpURLConnection c = (HttpURLConnection) u.openConnection();
	    c.setRequestMethod("GET");
	    c.setRequestProperty("Content-length", "0");
	    c.setUseCaches(false);
	    c.setAllowUserInteraction(false);
	    c.setConnectTimeout(3000);
	    c.setReadTimeout(3000);
	    c.connect();
	    int status = c.getResponseCode();
	    String json = "";
	    switch (status) {
	        case 200:
	        case 201:
	            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
	            StringBuilder sb = new StringBuilder();
	            String line;
	            while ((line = br.readLine()) != null) {
	                sb.append(line + "\n");
	            }
	            br.close();
	            json = sb.toString();
	    }
	    ObjectMapper mapper = new ObjectMapper();
	    ApplicationContainer[] cont = mapper.readValue(json.getBytes(), ApplicationContainer[].class);
	    
	    assertNotNull(cont);
	    assertTrue(cont.length > 0);
	}

}
