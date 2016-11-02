package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class MetaExchangeClientHandler {
//	private final FSM fsm;

	// NOT USED AT THE MOMENT
		
//	public MetaExchangeClientHandler(FSM fsm) {
//		this.fsm = fsm;
//	}
//
//	public MessageLite enterMex(Event e) {
//		
//		// TODO Declare which meta data attributes we want from provider
//		ByteString attrs = ByteString.EMPTY;
//		
//		return IdsProtocolMessages
//				.EnterMexReq
//				.newBuilder()
//				.setType(MessageType.ENTER_MEX_REQUEST)
//				.setYourAttributes(attrs)
//				.build();	
//
//	}
//
//	public MessageLite sendClientValues(Event e) {
//		// TODO Send our meta data values
//		ByteString values = ByteString.EMPTY;
//
//		return IdsProtocolMessages
//				.MexCMyValues
//				.newBuilder()
//				.setType(MessageType.MEX_C_MY_VALUES)
//				.setMyValues(values)
//				.build();	
//	}
}
