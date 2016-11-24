package de.fhg.camel.ids.comm.unixsocket;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
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
	
    private static final String SOCKET_FILE = "control.sock";
    private static final File LOCAL_SOCKET = new File("tpm2sim/socket/" + SOCKET_FILE);
    private static final String REMOTE_SOCKET = "/data/cml/communication/tpm2d/" + SOCKET_FILE;
    private static final String DOCKER_CLI = "docker";
    private static final String DOCKER_CONTAINER = "registry.netsec.aisec.fraunhofer.de/ids/tpm2dsim:latest";
	private static UnixSocketThread client;
	private static Thread thread;
	private static UnixSocketResponsHandler handler;
	private static File socketClient;

	/*
	@BeforeClass
    public static void initTest() throws InterruptedException {
		String sourceFolder = LOCAL_SOCKET.getAbsolutePath().substring(0, LOCAL_SOCKET.getAbsolutePath().length() - SOCKET_FILE.length());
		String targetFolder = REMOTE_SOCKET.substring(0, REMOTE_SOCKET.length() - SOCKET_FILE.length());

		Process p = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "run", "-i", "--rm", "--name", "tpm2dsim", "-v", sourceFolder +":"+targetFolder, DOCKER_CONTAINER, "/tpm2d/start.sh")).start();
        p.waitFor(660, TimeUnit.SECONDS);
    }
    
	@AfterClass
	public static void endTest() throws IOException, InterruptedException {
		Process p = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "stop" , DOCKER_CONTAINER)).start();
        p.waitFor(660, TimeUnit.SECONDS);
		thread.interrupt();
		thread = null;
	}
	*/
	
	@Before
    public void initThread() throws InterruptedException {
		try {
			// client will be used to send messages
			client = new UnixSocketThread(UnixSocketTest.REMOTE_SOCKET);
			thread = new Thread(client);
			//this.thread.setDaemon(true);
			thread.start();
			// responseHandler will be used to wait for messages
			handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			System.out.println("could not write to/read from " + REMOTE_SOCKET);
			e.printStackTrace();
		}
    }	
	
    @Test
    public void testSocketConnection() throws Exception {
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
