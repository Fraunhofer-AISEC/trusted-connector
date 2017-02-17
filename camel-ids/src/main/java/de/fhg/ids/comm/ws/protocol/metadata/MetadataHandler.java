package de.fhg.ids.comm.ws.protocol.metadata;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.ids.docker.Docker;

public class MetadataHandler {
	protected static Logger LOG = LoggerFactory.getLogger(MetadataHandler.class);
	protected static String lastError = "";
	
	public List<String> getMetaData() {
		Docker docker = new Docker();
		return docker.getMetaData();
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