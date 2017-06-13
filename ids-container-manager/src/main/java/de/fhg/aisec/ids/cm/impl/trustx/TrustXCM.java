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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
		
	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		sendCommand(Command.LIST_CONTAINERS);
		
		// TODO 
		return null;
	}

	@Override
	public void wipe(String containerID) {
        sendCommand(Command.CONTAINER_WIPE);
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
	
    /**
     * Used for sending control commands to a device. 
     *  
     * @param device The device the message is to be sent to.
     * @param command The command to be sent.
     * @return Success state. 
     */
    private boolean sendCommand(Command command){
        ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
        ctdmsg.setCommand(command);
        LOG.debug("sending message " + ctdmsg.getCommand());
        
        try{            
            
            //TODO Open Socket and get correct OutputStream
            DataOutputStream outputStream = new DataOutputStream(new OutputStream() {
				
				@Override
				public void write(int b) throws IOException {
					// TODO Auto-generated method stub
					
				}
			});
            byte[] encodedMessage = ctdmsg.build().toByteArray();
            outputStream.writeInt(encodedMessage.length);
            outputStream.write(encodedMessage);
        } catch (IOException ioe) {
            LOG.error("IOException occured:", ioe);
            // some error with the socket occured, so we get rid of the handler
        }
       return true;

    }
	
}
