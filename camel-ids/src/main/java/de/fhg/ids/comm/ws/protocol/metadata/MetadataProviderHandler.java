package de.fhg.ids.comm.ws.protocol.metadata;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.MedadataExchange;
import de.fhg.ids.comm.ws.protocol.fsm.Event;

public class MetadataProviderHandler extends MetadataHandler {
	
	public MessageLite request(Event e) {
		this.sessionID = e.getMessage().getId();
		this.yourKeys = e.getMessage().getMetadataExchange().getKeyList();
		this.myValues = this.generateMetaData(this.yourKeys);
		return ConnectorMessage
				.newBuilder()
				.setId(++this.sessionID)
				.setType(ConnectorMessage.Type.META_REQUEST)
				.setMetadataExchange(
						MedadataExchange
						.newBuilder()
						.addAllKey(myKeys)
						.addAllValue(myValues)
						.build())
				.build();
	}
}
