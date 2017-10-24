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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.Control.ControllerToDaemon;
import de.fhg.aisec.ids.Control.ControllerToDaemon.Command;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketThread;

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
	
	private static final Logger LOG = LoggerFactory.getLogger(TrustXCM.class);
	
	private static final String SOCKET = "/dev/socket/cml-control";
	private TrustmeUnixSocketThread socketThread;
	private TrustmeUnixSocketResponseHandler responseHandler;
	
	public TrustXCM() {
		this(SOCKET);
	}
	
	public TrustXCM(String socket) {
		super();
		try {
			this.socketThread = new TrustmeUnixSocketThread(socket);
			this.responseHandler = new TrustmeUnixSocketResponseHandler();
			Thread t = new Thread(socketThread);
			t.setDaemon(true);
			t.start();
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
		}
	}
	
	
	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		List<ApplicationContainer> result = new ArrayList<>();
		byte[] response = sendCommandAndWaitForResponse(Command.LIST_CONTAINERS);
		LOG.debug("Received response from cml: " + new String(response));
		
		return result;
	}

	@Override
	public void wipe(String containerID) {
        sendCommand(Command.CONTAINER_WIPE);
	}

	@Override
	public void startContainer(String containerID) {
		sendCommand(Command.CONTAINER_START);
	}

	@Override
	public void stopContainer(String containerID) {
		sendCommand(Command.CONTAINER_STOP);
	}


	@Override
	public void restartContainer(String containerID) {
		sendCommand(Command.CONTAINER_STOP);
		sendCommand(Command.CONTAINER_START);
	}

	@Override
	public Optional<String>  pullImage(String imageID) {
		// TODO Auto-generated method stub
		return Optional.<String>empty();

	}

	public static boolean isSupported() {
		Path path = Paths.get(SOCKET);
		boolean exists = false;
		if (Files.exists(path)) {
		  exists = true;
		}
		return exists;
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
	
    /**
     * Used for sending control commands to a device. 
     *  
     * @param command The command to be sent.
     */
    private void sendCommand(Command command){
        ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
        ctdmsg.setCommand(command).build().toByteArray();
        LOG.debug("sending message " + ctdmsg.getCommand());
        byte[] encodedMessage = ctdmsg.build().toByteArray();
    	try {
			socketThread.sendWithHeader(encodedMessage, responseHandler);
		} catch (IOException | InterruptedException e) {
			LOG.error(e.getMessage(),e);
		}
    }
    
    /**
     * Used for sending control commands to a device. 
     *  
     * @param command The command to be sent.
     * @return Success state. 
     */
    private byte[] sendCommandAndWaitForResponse(Command command){
    		sendCommand(command);
    		byte[] response = responseHandler.waitForResponse();
    		return response;
    }
	
}
