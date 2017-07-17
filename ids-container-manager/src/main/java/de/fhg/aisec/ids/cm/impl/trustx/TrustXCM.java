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
package de.fhg.aisec.ids.cm.impl.trustx;

import java.util.List;
import java.util.Optional;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;

/**
 * ContainerManager implementation for trust-x containers.
 * 
 * /dev/socket/cml-control
 * Protobuf: control.proto
 * container.proto für container configs
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class TrustXCM implements ContainerManager {
		
	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void wipe(String containerID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startContainer(String containerID) {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopContainer(String containerID) {
		// TODO Auto-generated method stub
	}


	@Override
	public void restartContainer(String containerID) {
		// TODO Auto-generated method stub

	}

	@Override
	public Optional<String>  pullImage(String imageID) {
		// TODO Auto-generated method stub
		return Optional.<String>empty();

	}

	public static boolean isSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String inspectContainer(String containerID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMetadata(String containerID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIpRule(String containerID, Direction direction, int srcPort, int dstPort, String srcDstRange,
			Protocol protocol, Decision decision) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}
}
