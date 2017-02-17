package de.fhg.ids.comm.ws.protocol.metadata;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.MedadataExchange;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;


public class MetadataHandler {
	protected static Logger LOG = LoggerFactory.getLogger(MetadataHandler.class);
	protected static String lastError = "";
	
	public static List<String> getValues(List<String> yourKeys) {
		// TODO Auto-generated method stub
		return Arrays.asList("asd");
	}

	public static MessageLite sendError(String lastError) {
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage(lastError)
						.build())
				.build();
	}
}