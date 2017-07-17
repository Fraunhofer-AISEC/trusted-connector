/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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
package de.fhg.camel.ids.comm.ws.protocol;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import de.fhg.aisec.ids.messages.AttestationProtos.*;
import de.fhg.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.rat.NonceGenerator;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;

public class UnixSocketIT {

    @Test
    public void testBASIC() throws Exception {
    	
    	UnixSocketThread client;
    	Thread thread;
    	String socket = "socket/control.sock";
    	UnixSocketResponseHandler handler;
    	String quoted = NonceGenerator.generate(40);
    	IdsAttestationType type = IdsAttestationType.BASIC;
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(socket);
			thread = new Thread(client);
			thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponseHandler();
			
	    	// construct protobuf message to send to local tpm2d via unix socket
			ControllerToTpm msg = ControllerToTpm
					.newBuilder()
					.setAtype(type)
					.setQualifyingData(quoted)
					.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
					.build();
			client.send(msg.toByteArray(), handler, true);
			System.out.println("waiting for socket response ....");
			byte[] tpmData = handler.waitForResponse();
			System.out.println("tpmData length : " + tpmData.length);
			// and wait for response
			TpmToController response = TpmToController.parseFrom(tpmData);
			System.out.println(response.toString());
			assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
			assertTrue(response.getAtype().equals(type));
			
		} catch (IOException e) {
			System.out.println("could not write to/read from " + socket);
			e.printStackTrace();
		}
    }

    @Test
    public void testALL() throws Exception {

    	UnixSocketThread client;
    	Thread thread;
    	String socket = "socket/control.sock";
    	UnixSocketResponseHandler handler;
    	String quoted = NonceGenerator.generate(40);
    	IdsAttestationType type = IdsAttestationType.ALL;
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(socket);
			thread = new Thread(client);
			thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponseHandler();
			
	    	// construct protobuf message to send to local tpm2d via unix socket
			ControllerToTpm msg = ControllerToTpm
					.newBuilder()
					.setAtype(type)
					.setQualifyingData(quoted)
					.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
					.build();
			client.send(msg.toByteArray(), handler, true);
			System.out.println("waiting for socket response ....");
			byte[] tpmData = handler.waitForResponse();
			System.out.println("tpmData length : " + tpmData.length);
			// and wait for response
			TpmToController response = TpmToController.parseFrom(tpmData);
			System.out.println(response.toString());
			assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
			assertTrue(response.getAtype().equals(type));
			
		} catch (IOException e) {
			System.out.println("could not write to/read from " + socket);
			e.printStackTrace();
		}
    }

    @Test
    public void testADVANCED() throws Exception {

    	UnixSocketThread client;
    	Thread thread;
    	String socket = "socket/control.sock";
    	UnixSocketResponseHandler handler;
    	String quoted = NonceGenerator.generate(40);
    	IdsAttestationType type = IdsAttestationType.ADVANCED;
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(socket);
			thread = new Thread(client);
			thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponseHandler();
			
	    	// construct protobuf message to send to local tpm2d via unix socket
			ControllerToTpm msg = ControllerToTpm
					.newBuilder()
					.setAtype(type)
					.setQualifyingData(quoted)
					.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
					.setPcrs(24)
					.build();
			client.send(msg.toByteArray(), handler, true);
			System.out.println("waiting for socket response ....");
			byte[] tpmData = handler.waitForResponse();
			System.out.println("tpmData length : " + tpmData.length);
			// and wait for response
			TpmToController response = TpmToController.parseFrom(tpmData);
			System.out.println(response.toString());
			assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
			assertTrue(response.getAtype().equals(type));
			
		} catch (IOException e) {
			System.out.println("could not write to/read from " + socket);
			e.printStackTrace();
		}
    }
}
