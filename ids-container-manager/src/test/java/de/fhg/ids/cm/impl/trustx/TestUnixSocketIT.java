package de.fhg.ids.cm.impl.trustx;

import java.io.IOException;

import org.junit.Test;

import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketThread;

public class TestUnixSocketIT {

	
	
	@Test
	public void testServer() throws IOException, InterruptedException{
		TrustmeUnixSocketThread client = new TrustmeUnixSocketThread("src/test/socket/trustme.sock");
		Thread t = new Thread(client);
		t.setDaemon(true);
		t.start();

		TrustmeUnixSocketResponseHandler handler = new TrustmeUnixSocketResponseHandler();
		String data = "An iterator over a collection. Iterator takes the place of Enumeration in the Java Collections Framework. Iterators differ from enumerations in two ways: Iterators allow the caller to remove elements from the underlying collection during the iteration with well-defined semantics. Method names have been improved. This interface is a member of the Java Collections Framework.";
		client.send(data.getBytes(), handler);
		
		handler.waitForResponse();
	}
	
    public void testBASIC() throws Exception {
    	
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
