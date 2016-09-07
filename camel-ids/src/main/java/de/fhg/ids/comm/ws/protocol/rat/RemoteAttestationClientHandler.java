package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.RatType;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationClientHandler {
	private final FSM fsm;
	
	public RemoteAttestationClientHandler(FSM fsm) {
		this.fsm = fsm;
	}

	public String enterRat(Event e) {
		return "entering rat";
	}

	public MessageLite handleEnterRatRequest(Event e) {
		return IdsProtocolMessages
				.EnterRatReq
				.newBuilder()
				.setType(RatType.ENTER_RAT_REQUEST)
				.build();
	}

	public MessageLite sendClientIdAndNonce(Event e) {		
		// TODO retrieve client id from TPM
		ByteString client_id = ByteString.EMPTY;
		
		// TODO retrieve client nonce from TPM
		ByteString client_nonce = ByteString.EMPTY;
		
		return IdsProtocolMessages
				.RatCMyNonce
				.newBuilder()
				.setType(RatType.RAT_C_MY_NONCE)
				.setIdC(client_id)
				.setNonceC(client_nonce)
				.build();
	}

	public MessageLite sendPcr(Event e) {				
		// TODO retrieve measurement list from TPM
		ByteString ml = ByteString.EMPTY;
		
		// TODO retrieve AIK_CERT from TPM
		ByteString aik_cert = ByteString.EMPTY;
		
		// TODO retrieve pcr from TPM
		ByteString pcr = ByteString.EMPTY;
		
		return IdsProtocolMessages
				.RatCReqPcr
				.newBuilder()
				.setType(RatType.RAT_C_REQ_PCR)
				.setMeasurementList(ml)
				.setAikCert(aik_cert)
				.setSignedPcr(pcr)
				.build();	
	}

	public MessageLite sendSignedServerNonce(Event e) {		
		// TODO sign nonce from P and sign it
		ByteString nonce_signed = ByteString.EMPTY;
		
		return IdsProtocolMessages
				.RatCYourNonce
				.newBuilder()
				.setType(RatType.RAT_C_YOUR_NONCE)
				.setSignedNonceP(nonce_signed )
				.build();	
	}

}
