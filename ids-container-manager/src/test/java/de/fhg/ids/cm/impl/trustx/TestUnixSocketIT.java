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
package de.fhg.ids.cm.impl.trustx;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.fhg.aisec.ids.Control.ControllerToDaemon;
import de.fhg.aisec.ids.Control.ControllerToDaemon.Command;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketThread;
import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;


@RunWith(MockitoJUnitRunner.class)
public class TestUnixSocketIT {

	private static final String socket = "src/test/socket/trustme.sock";
	
	@InjectMocks
	private TrustXCM trustXmanager = new TrustXCM(socket);
	
	@Mock
	private TrustmeUnixSocketThread mockSocket = mock(TrustmeUnixSocketThread.class);
	
	@Mock
	private TrustmeUnixSocketResponseHandler mockHandler = mock(TrustmeUnixSocketResponseHandler.class);

	@BeforeClass
	public static void setup() throws IOException {
		File socketFile = new File(socket);
		socketFile.delete();
		socketFile.deleteOnExit();
		
		UnixSocketAddress address = new UnixSocketAddress(socketFile.getAbsoluteFile());	
		UnixServerSocketChannel channel = UnixServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(address);		
	}
	
	@Test
	public void testServer() throws IOException, InterruptedException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException, InvocationTargetException, NoSuchMethodException{

		// prepare access to internal state pendingData
		Object cc = TrustmeUnixSocketThread.class.getConstructor(String.class).newInstance(socket);
        Field f = cc.getClass().getDeclaredField("pendingData");
        f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<UnixSocketChannel, List<ByteBuffer>> internalState = (Map<UnixSocketChannel, List<ByteBuffer>>)f.get(cc);

		// prepare data to send
		ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
		byte[] data = ctdmsg.setCommand(Command.CONTAINER_START).build().toByteArray();

		// check that pendingData is empty
        Assert.assertTrue(internalState.isEmpty());
		
        // now here's the test, we send our protobuf message
        ((TrustmeUnixSocketThread)cc).sendWithHeader(data, mockHandler);
		
        // check that the message in pendingData has the length header
        int count = 0;
        for (List<ByteBuffer> list: internalState.values()) {
        		ByteBuffer result = list.get(0);
        		byte[] length = new byte[4];        		
        		System.arraycopy(result.array(), 0, length, 0, 4);
        		byte[] message = new byte[result.array().length - 4];
        		System.arraycopy(result.array(), 4, message, 0, message.length);
        		
        		
        		Assert.assertTrue("Length Header not correct", message.length == (new BigInteger(length)).intValue());
        		Assert.assertTrue("Message not correct", Arrays.equals(data, message));
        		count = count + 1;
        }
        Assert.assertTrue("Unexpected number of items in Buffer", count == 1);
	}
	
	//@Test
    public void testBASIC() throws Exception {    	
    	List<ApplicationContainer> resultList = trustXmanager.list(true);
    	ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
    ctdmsg.setCommand(Command.LIST_CONTAINERS).build().toByteArray();
    byte[] encodedMessage = ctdmsg.build().toByteArray();
    	verify(mockSocket).sendWithHeader(encodedMessage, mockHandler);
    	
    	
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
