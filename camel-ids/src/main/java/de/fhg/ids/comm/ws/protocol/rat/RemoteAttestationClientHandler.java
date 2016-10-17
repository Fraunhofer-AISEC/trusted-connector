package de.fhg.ids.comm.ws.protocol.rat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.RatType;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationClientHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private byte[] myNonce;
	private byte[] yourNonce;
	
	public RemoteAttestationClientHandler(FSM fsm) {
		this.fsm = fsm;
		this.myNonce = this.setNonce();
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
		ByteString client_nonce = ByteString.copyFrom(this.myNonce);
		return IdsProtocolMessages
				.RatCMyNonce
				.newBuilder()
				.setType(RatType.RAT_C_MY_NONCE)
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
