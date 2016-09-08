package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.RatType;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationServerHandler {
	private final FSM fsm;

	public RemoteAttestationServerHandler(FSM fsm) {
		this.fsm = fsm;
	}

	public MessageLite replyToRatRequest(Event e) {
		return IdsProtocolMessages
				.EnterRatResp
				.newBuilder()
				.setType(RatType.ENTER_RAT_RESPONSE).build();
	}

	public MessageLite sendServerNonceAndCert(Event e) {
		// TODO get nonce from TPM
		ByteString nonce = ByteString.EMPTY;
		
		// TODO get cert (a certificate for my public key, which is signed by a CA which must be trusted by the client and also contains my ID)
		ByteString cert = ByteString.EMPTY;
		return IdsProtocolMessages
				.RatPMyNonce
				.newBuilder()
				.setType(RatType.RAT_P_MY_NONCE)
				.setNonceP(nonce)
				.setCertP(cert)
				.build();
	}

	/**
	 * Confirms successful run of the rat protocol by sending back the client's nonce and AIK-public key, signed with our private key.
	 * 
	 * @param e
	 * @return
	 */
	public MessageLite sendSignedClientNonce(Event e) {
		
		// TODO sign nonce retrieved from client and AIK-public key from client with my private key
		ByteString signed_client_nonce = ByteString.EMPTY;
		
		return IdsProtocolMessages
				.RatPYourNonce
				.newBuilder()
				.setType(RatType.RAT_P_YOUR_NONCE)
				.setSignedNonceC(signed_client_nonce)
				.build();	
	}

	/**
	 * Send "leaving rat" message to client.
	 * 
	 * @param e
	 * @return
	 */
	public MessageLite leaveRat(Event e) {
		return IdsProtocolMessages
				.RatLeave
				.newBuilder()
				.setType(RatType.RAT_LEAVE)
				.build();	
	}


}
