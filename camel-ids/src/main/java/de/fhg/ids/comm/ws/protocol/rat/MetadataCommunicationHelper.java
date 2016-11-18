package de.fhg.ids.comm.ws.protocol.rat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.MedadataExchange;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;


public class MetadataCommunicationHelper {
	private Logger LOG = LoggerFactory.getLogger(MetadataCommunicationHelper.class);

	public MessageLite buildMetaDataResponse(Event e) {
		//TODO: Send real metadata
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.META_DATA_RESPONSE)
				.setMetadataExchange(
						MedadataExchange
						.newBuilder()
						.setMetadata("SEND THIS METADATA STRING IN RESPONSE")
						.build())
				.build();
	}
	
	public MessageLite buildMetaDataRequest(Event e) {
		//TODO: Send real metadata
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.META_DATA_REQUEST)
				.setMetadataExchange(
						MedadataExchange
						.newBuilder()
						.setMetadata("SEND THIS METADATA STRING IN REQUEST")
						.build())
				.build();
	}

	public boolean parseMetadata(Event e) {
		
		//TODO: What to do with the metadata?
		return true;
	}
	

}
