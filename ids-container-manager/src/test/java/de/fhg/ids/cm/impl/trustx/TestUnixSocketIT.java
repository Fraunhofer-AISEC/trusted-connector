package de.fhg.ids.cm.impl.trustx;

import java.io.IOException;
import java.util.List;

import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import de.fhg.aisec.ids.Control.ControllerToDaemon;
import de.fhg.aisec.ids.Control.ControllerToDaemon.Command;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketThread;

@RunWith(MockitoJUnitRunner.class)
public class TestUnixSocketIT {

	private static final String socket = "src/test/socket/trustme.sock";
	
	@InjectMocks
	private TrustXCM trustXmanager = new TrustXCM(socket);
	
	@Mock
	private TrustmeUnixSocketThread mockSocket = mock(TrustmeUnixSocketThread.class);
	
	@Mock
	private TrustmeUnixSocketResponseHandler mockHandler = mock(TrustmeUnixSocketResponseHandler.class);
	
	@Test
	public void testServer() throws IOException, InterruptedException{
		TrustmeUnixSocketThread client = new TrustmeUnixSocketThread(socket);
		Thread t = new Thread(client);
		t.setDaemon(true);
		t.start();

		TrustmeUnixSocketResponseHandler handler = new TrustmeUnixSocketResponseHandler();
		
		String data = "An iterator over a collection. Iterator takes the place of Enumeration in the Java Collections Framework. Iterators differ from enumerations in two ways: Iterators allow the caller to remove elements from the underlying collection during the iteration with well-defined semantics. Method names have been improved. This interface is a member of the Java Collections Framework.";
		client.send(data.getBytes(), handler);
		
		handler.waitForResponse();
		System.out.println("probably got response");
	}
	
	@Test
    public void testBASIC() throws Exception {    	
    	List<ApplicationContainer> resultList = trustXmanager.list(true);
    	ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
    ctdmsg.setCommand(Command.LIST_CONTAINERS).build().toByteArray();
    byte[] encodedMessage = ctdmsg.build().toByteArray();
    	verify(mockSocket).send(encodedMessage, mockHandler);
    	
    	
//    	UnixSocketThread client;
//    	Thread thread;
//    	String socket = "socket/control.sock";
//    	UnixSocketResponseHandler handler;
//    	try {
//			// client will be used to send messages
//			client = new UnixSocketThread(socket);
//			thread = new Thread(client);
//			thread.setDaemon(true);
//			thread.start();
//			// responseHandler will be used to wait for messages
//			handler = new UnixSocketResponseHandler();
//			
//	    	// construct protobuf message to send to local tpm2d via unix socket
//			ControllerToTpm msg = ControllerToTpm
//					.newBuilder()
//					.setAtype(type)
//					.setQualifyingData(quoted)
//					.setCode(ControllerToTpm.Code.INTERNAL_ATTESTATION_REQ)
//					.build();
//			client.send(msg.toByteArray(), handler, true);
//			System.out.println("waiting for socket response ....");
//			byte[] tpmData = handler.waitForResponse();
//			System.out.println("tpmData length : " + tpmData.length);
//			// and wait for response
//			TpmToController response = TpmToController.parseFrom(tpmData);
//			System.out.println(response.toString());
//			assertTrue(response.getCode().equals(TpmToController.Code.INTERNAL_ATTESTATION_RES));
//			assertTrue(response.getAtype().equals(type));
//			
//		} catch (IOException e) {
//			System.out.println("could not write to/read from " + socket);
//			e.printStackTrace();
//		}
    }

}
