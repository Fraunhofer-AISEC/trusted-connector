/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.webconsole.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.NoContainerExistsException;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for managing "apps" in the connector.
 * 
 * In this implementation, apps are either docker or trustX containers. 
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/apps/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/apps")
public class AppApi {
	private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);
	
	@GET
	@Path("list")
	@Produces("application/json")
	public List<ApplicationContainer> list() {
		List<ApplicationContainer> result = new ArrayList<>();
		
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		if (cml.isPresent()) {
			result = cml.get().list(false);
		}
		
		result.sort((app1, app2) -> {
			try {
				SimpleDateFormat d = new SimpleDateFormat("dd-MM-yyyy HH:mm:s Z");
				Date date1 = d.parse(app1.getCreated());
				Date date2 = d.parse(app2.getCreated());
				if (date1.getTime()<date2.getTime()) {
					return 1;
				} else {
					return -1;
				}
			} catch (Throwable t) {
				LOG.warn("Unexpected app creation date/time. Cannot sort. " + t.getMessage());
			}
			return 0;
		});
		return result;
	}
	
	@POST
	@Path("pull")
	@Produces("application/json")
	public boolean pull(@QueryParam("imageId") String imageId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return false;
		}

		new Thread() {
			@Override
			public void run() {
				try {
					if (cml.isPresent()) {
						cml.get().pullImage(imageId);
					}
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);					
				}
			}
		}.start();			
		return true;
	}
	
	@GET
	@Path("start")
	@Produces("application/json")
	public boolean start(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().startContainer(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}

	@GET
	@Path("stop")
	@Produces("application/json")
	public boolean stop(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().stopContainer(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}

	@GET
	@Path("wipe")
	@Produces("application/json")
	public boolean wipe(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().wipe(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return false;
	}

	@GET
	@Path("cml_version")
	@Produces("application/json")
	public Map<String,String> getCml() {
		Map<String,String> result = new HashMap<>();
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return result;
		}
		
		result.put("cml_version", cml.get().getVersion());
		return result;
	}	
}