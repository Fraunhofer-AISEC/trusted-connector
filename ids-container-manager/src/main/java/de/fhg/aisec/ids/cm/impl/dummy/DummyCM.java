/*-
 * ========================LICENSE_START=================================
 * IDS Container Manager
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
package de.fhg.aisec.ids.cm.impl.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;

/**
 * Dummy implementation of a null container manager which is used if no real CMLd is available.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class DummyCM implements ContainerManager {
	private static final Logger LOG = LoggerFactory.getLogger(DummyCM.class);

	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		return new ArrayList<>();
	}

	@Override
	public void wipe(final String containerID) {   }

	@Override
	public void startContainer(final String containerID) {  }

	@Override
	public void stopContainer(final String containerID) {  }


	@Override
	public void restartContainer(final String containerID) { }

	@Override
	public Optional<String> pullImage(final String imageID) { return Optional.empty(); }

	@Override
	public Map<String, String> getMetadata(String containerID) {
		return new HashMap<>();
	}

	@Override
	public void setIpRule(String containerID, Direction direction, int srcPort, int dstPort, String srcDstRange,
			Protocol protocol, Decision decision) { 	}

	@Override
	public String inspectContainer(final String containerID) { return "";	}


	@Override
	public String getVersion() { return "no cmld installed"; }
}