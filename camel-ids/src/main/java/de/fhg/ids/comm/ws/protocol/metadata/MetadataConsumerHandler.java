package de.fhg.ids.comm.ws.protocol.metadata;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.MedadataExchange;
import de.fhg.ids.comm.ws.protocol.fsm.Event;

public class MetadataConsumerHandler extends MetadataHandler {

	public MessageLite request(Event e) {
		this.sessionID = e.getMessage().getId();
		return ConnectorMessage
				.newBuilder()
				.setId(++this.sessionID)
				.setType(ConnectorMessage.Type.META_REQUEST)
				.setMetadataExchange(
						MedadataExchange
						.newBuilder()
						.addAllKey(myKeys)
						.build())
				.build();
	}
	
	public MessageLite response(Event e) {
		if(++this.sessionID == e.getMessage().getId()) {
			this.yourKeys = e.getMessage().getMetadataExchange().getKeyList();
			this.yourValues = e.getMessage().getMetadataExchange().getValueList();
			this.myValues = this.generateMetaData(this.yourKeys);
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.META_RESPONSE)
					.setMetadataExchange(
							MedadataExchange
							.newBuilder()
							.addAllValue(myValues)
							.build())
					.build();
			
		}
		else {
			return MetadataHandler.sendError("error: sessionID's do not match");
		}
	}
}
