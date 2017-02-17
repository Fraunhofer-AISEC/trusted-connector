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
	
	private UnixSocketThread client;
	private Thread thread;
	private static final String socket = "socket/sim1/control.sock";
	private static UnixSocketResponseHandler handler;

    @Test
    public void testSocketConnection() throws Exception {
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(socket);
			thread = new Thread(client);
			thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponseHandler();
		} catch (IOException e) {
			System.out.println("could not write to/read from " + socket);
			e.printStackTrace();
		}
    	// construct protobuf message to send to local tpm2d via unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(IdsAttestationType.BASIC)
				.setQualifyingData(NonceGenerator.generate())
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
    }
}
