package de.fhg.ids.comm.ws.protocol.metadata;

import java.util.Arrays;
import java.util.List;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.MedadataExchange;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationHandler;
import de.fhg.ids.docker.Docker;

public class MetadataProviderHandler extends MetadataHandler {
	private long sessionID = -1;
	// all metadata keys i want to know from you
	private List<String> myKeys = Arrays.asList();
	// all metadata keys you want to know from me
	private List<String> yourKeys = Arrays.asList();
	// all metadata values i provide to you
	private List<String> myValues = Arrays.asList();
	// all metadata values you provide to me	
	private List<String> yourValues = Arrays.asList();

		
	public MessageLite request(Event e) {
		this.sessionID = e.getMessage().getId();
		this.yourKeys = e.getMessage().getMetadataExchange().getKeysList();
		//this.myValues = getMetaData();
		return ConnectorMessage
				.newBuilder()
				.setId(++this.sessionID)
				.setType(ConnectorMessage.Type.META_REQUEST)
				.setMetadataExchange(
						MedadataExchange
						.newBuilder()
						.addAllKeys(myKeys)
						.addAllValues(myValues)
						.build())
				.build();
	}
	
	public MessageLite response(Event e) {
		if(++this.sessionID == e.getMessage().getId()) {
			this.yourValues = e.getMessage().getMetadataExchange().getValuesList();
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.META_RESPONSE)
					.setMetadataExchange(
							MedadataExchange
							.newBuilder()
							.addAllValues(myValues)
							.build())
					.build();
		}
		else {
			return MetadataHandler.sendError("error: sessionID's do not match");
		}
	}
	
	public MessageLite leave(Event e) {
		if(++this.sessionID == e.getMessage().getId()) {
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.META_LEAVE)
					.build();
		}
		else {
			return MetadataHandler.sendError("error: sessionID's do not match");
		}
	}
}
