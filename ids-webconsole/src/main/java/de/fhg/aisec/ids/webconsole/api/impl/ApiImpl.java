package de.fhg.aisec.ids.webconsole.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.spi.UriParam;

import com.google.gson.Gson;

import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.Api;
import de.fhg.aisec.ids.webconsole.api.Bundle;


/**
 * REST API for IDS Core Platform.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class ApiImpl implements Api {

	@Override
	public String getConfig() {
		//TODO Just for testing: Map to JSON
		Map<String, String> result = new HashMap<String, String>();
		result.put("test", "test");
		Gson gson = new Gson();
		return gson.toJson(result);
	}
	
	@Override
	public String listContainers() {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		if (cml==null) {
			return new Gson().toJson("false");
		}
		
		return new Gson().toJson(cml.list(false));		
	}

}