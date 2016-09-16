package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.MessageType;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class MetaExchangeServerHandler {
	private final FSM fsm;
	
	public MetaExchangeServerHandler(FSM fsm) {
		this.fsm = fsm;
	}

	public MessageLite sendEnterMexResponse(Event e) {

		// TODO Set our meta data values requested by client
		ByteString values = ByteString.EMPTY;

		// TODO Set attributes requested from client
		ByteString attrs = ByteString.EMPTY;

		return IdsProtocolMessages
				.EnterMexResp
				.newBuilder()
				.setType(MessageType.ENTER_MEX_RESPONSE)
				.setMyValues(values)
				.setYourAttributes(attrs)
				.build();	
	}

	public MessageLite sendLeave(Event e) {
		return IdsProtocolMessages
				.MexLeave
				.newBuilder()
				.setType(MessageType.MEX_LEAVE)
				.build();	
	}
}
