package de.fhg.camel.ids.comm.unixsocket;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.messages.AttestationProtos.*;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.rat.NonceGenerator;

public class UnixSocketTest {
	
    private static String DOCKER_CLI ="docker";
    private static String DOCKER_IMAGE = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dsim:latest";
    private static String SOCKET = "control.sock";
    private static String SOCKET_PATH = "tpm2sim/socket/" + SOCKET;
	private static UnixSocketThread client;
	private static Thread thread;
	private static UnixSocketResponsHandler handler;
	private static File socketFile;

	@BeforeClass
    public static void initSimServer() throws InterruptedException, IOException {
		socketFile = new File(SOCKET_PATH);
		String folder = socketFile.getAbsolutePath().substring(0, socketFile.getAbsolutePath().length() - SOCKET.length());
		// pull the image
		Process p1 = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "build", "-t", DOCKER_IMAGE, "./tpm2sim/")).start();
		p1.waitFor(4, TimeUnit.SECONDS);
		System.out.println(getInputAsString(p1.getInputStream()));
		//System.out.println(UnixSocketTest.getInputAsString(p1.getInputStream()));
    	// then start the docker image
		UnixSocketTest.kill("ust");
		Process p2 = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "run", "--name", "ust", "-v", folder +":/data/cml/communication/tpm2d/", DOCKER_IMAGE, "/tpm2d/start.sh")).start();
		p2.waitFor(4, TimeUnit.SECONDS);
		System.out.println(getInputAsString(p2.getInputStream()));
		//System.out.println(UnixSocketTest.getInputAsString(p2.getInputStream()));
    }
	
	@AfterClass
    public static void teardownSimServer() throws Exception {
		UnixSocketTest.kill("ust");
		socketFile.delete();
    }
	
	private static String getInputAsString(InputStream is) {
	   try(java.util.Scanner s = new java.util.Scanner(is))  { 
	       return s.useDelimiter("\\A").hasNext() ? s.next() : ""; 
	   }
	}
	
	private static void kill(String id) throws InterruptedException, IOException {
		// pull the image
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "stop", id)).start().waitFor(2, TimeUnit.SECONDS);
    	// pull the image
		new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "rm", id)).start().waitFor(2, TimeUnit.SECONDS);
	}
		
    @Test
    public void testSocketConnection() throws Exception {
    	try {
			// client will be used to send messages
			client = new UnixSocketThread(UnixSocketTest.SOCKET_PATH);
			thread = new Thread(client);
			//this.thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			System.out.println("could not write to/read from " + SOCKET_PATH);
			e.printStackTrace();
		}
    	// construct protobuf message to send to local tpm2d via unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(IdsAttestationType.BASIC)
				.setQualifyingData(NonceGenerator.generate())
				.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
				.build();
		client.send(msg.toByteArray(), UnixSocketTest.handler);
		// and wait for response
		TpmToController response = TpmToController.parseFrom(UnixSocketTest.handler.waitForResponse());
		//System.out.println(response.toString());
		assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
    }
}
