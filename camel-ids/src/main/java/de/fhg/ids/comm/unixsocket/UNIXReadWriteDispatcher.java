package de.fhg.ids.comm.unixsocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import jnr.unixsocket.UnixSocketChannel;

/**
 * Handles incoming messages on UNIX domain socket channels.
 * 
 * @author Georg Räß (georg.raess@aisec.fraunhofer.de)
 *
 */
public class UNIXReadWriteDispatcher implements Dispatcher {
	private Logger LOG = LoggerFactory.getLogger(UNIXReadWriteDispatcher.class);
	private final UnixSocketChannel channel;
	private static ControllerToTpm dataOut = null;
	
	public UNIXReadWriteDispatcher(UnixSocketChannel channel) {
		this.channel = channel;
	}

	public static void write(ControllerToTpm msg) {
		dataOut = msg;
	}

	/**
	 * Try to read up to 1024 byte from channel, build a protobuf message from it, and handle the message.
	 * 
	 * This method is called by ServerThread when data is available from the channel.
	 */
	@Override
	public boolean receive() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			int bytesRead = channel.read(buffer);

			if (bytesRead == 0) {
				LOG.debug("For some reason we have read 0 bytes. Happens, just ignore it and wait for the next read");
				return true;
			}
			
			// -1 bytes indicate closed channel
			if (bytesRead == -1) {
				LOG.info("EOF. Closing channel");
				return false;
			}
			ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array());
			TpmToController msg = TpmToController.parseDelimitedFrom(bis);
			de.fhg.aisec.ids.messages.AttestationProtos.TpmToController.Code code = msg.getCode();
			IdsAttestationType attestationType = msg.getAtype();
			String hashAlgorithm = msg.getHalg();
			String quoted = msg.getQuoted();
			String signature = msg.getSignature();
			String certUri = msg.getCertificateUri();
			List<Pcr> pcrValues = msg.getPcrValuesList();
			for(int i = 0; i<pcrValues.size(); i++) {
				// the i'th pcr value
				Pcr value = pcrValues.get(i);
			}			
		} catch (InvalidProtocolBufferException e) {
			LOG.warn("Not a valid protobuf (yet).");
			return false;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * handle the message, build a protobuf message from it, and try to write up to 1024 byte to channel, 
	 * 
	 * This method is called by ClientThread when data is available from the channel.
	 */
	@Override
	public boolean dispatch() {
		if(dataOut != null) {
			PrintWriter w = new PrintWriter(Channels.newOutputStream(channel));
	        w.print(dataOut);
	        w.flush();
	        dataOut = null;
		}
		return true;
	}
}