package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.Idscp.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.rat.tpm.objects.TPM2B_PUBLIC;
import de.fhg.ids.comm.ws.protocol.rat.tpm.tools.PublicKeyConverter;

public class RemoteAttestationServerHandler {
	private final FSM fsm;
	private String SOCKET = "mock/tpm2ds.sock";
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private boolean attestationSucccessfull = false;
	private Thread thread;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationServerHandler.class);
	private UnixSocketThread client;
	private UnixSocketResponsHandler handler;
	private ByteString yourQuoted;
	private ByteString yourSignature;
	private String certUri;
	
	public RemoteAttestationServerHandler(FSM fsm, IdsAttestationType type) {
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type (see attestation.proto)
		this.aType = type;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d		
		try {
			// client will be used to send messages
			this.client = new UnixSocketThread(this.SOCKET);
			this.thread = new Thread(client);
			this.thread.setDaemon(true);
			this.thread.start();
			// responseHandler will be used to wait for messages
			this.handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			LOG.debug("could not write to/read from " + SOCKET);
			e.printStackTrace();
		}		
	}

	public MessageLite sendTPM2Ddata(Event e) {
		// generate a new software nonce on the server
		this.myNonce = NonceGenerator.generate();
		// get the current client nonce from the message
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData().toString();
		// set the current attestation to the same as the client
		this.aType = e.getMessage().getAttestationRequest().getAtype();
		// construct protobuf message to send to local tpm2d via unix socket
		ControllerToTpm msg = ControllerToTpm
				.newBuilder()
				.setAtype(this.aType)
				.setQualifyingData(this.yourNonce)
				.setCode(Code.INTERNAL_ATTESTATION_REQ)
				.build();		
		// try to talk to local unix socket
		try {
			if(thread.isAlive()) {
				// send msg to local unix socket
				client.send(msg.toByteArray(), this.handler);
				TpmToController answer = this.handler.waitForResponse();
				// now return values from answer to client
				return ConnectorMessage
						.newBuilder()
						.setId(0)
						.setType(ConnectorMessage.Type.RAT_RESPONSE)
						.setAttestationResponse(
								AttestationResponse
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.myNonce)
								.setHalg(answer.getHalg())
								.setQuoted(answer.getQuoted())
								.setSignature(answer.getSignature())
								.addAllPcrValues(answer.getPcrValuesList())
								.setCertificateUri(answer.getCertificateUri())
								.build()
								)
						.build();				
			}
			else {
				LOG.debug("error: thread is not alive");
				return null;
			}
		} catch (IOException e1) {
			LOG.debug("IOException when writing to unix socket");
			e1.printStackTrace();
			return null;
		} catch (InterruptedException e1) {
			LOG.debug("InterruptedException when writing to unix socket");
			e1.printStackTrace();
			return null;
		}
	}

	public MessageLite sendResult(Event e) {
		this.attestationSucccessfull  = false;
		
		this.yourQuoted = e.getMessage().getAttestationResponse().getQuotedBytes();
		this.yourSignature = e.getMessage().getAttestationResponse().getSignatureBytes();
		this.certUri = e.getMessage().getAttestationResponse().getCertificateUri();
		try {
			// construct a new public key in TPM2 format
			TPM2B_PUBLIC key = new TPM2B_PUBLIC();
			// build that key with bytes from certUri
			key.fromBytes(this.fetchPublicKey(this.certUri), 0);
			PublicKey publicKey = new PublicKeyConverter(key).getPublicKey();
			
			LOG.debug("RSA KEY recvd by SERVER: " + publicKey.toString());
			
			// CURRENTO TODO: now convert the TPM2 public key to a RSA DER Public key
			
		} catch (Exception ex) {
			LOG.debug("error: exception " + ex.getMessage());
			ex.printStackTrace();
		}
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_RESULT)
				.setAttestationResult(
						AttestationResult
						.newBuilder()
						.setAtype(this.aType)
						.setResult(this.attestationSucccessfull)
						.build()
						)
				.build();
	}

	private byte[] fetchPublicKey(String uri) throws Exception {
		URL cert = new URL(uri);
		BufferedReader in = new BufferedReader(new InputStreamReader(cert.openStream()));
		String base64 = "";
		String inputLine = "";
        while ((inputLine = in.readLine()) != null) {
        	base64 += inputLine;
        }
        in.close();
        return javax.xml.bind.DatatypeConverter.parseBase64Binary(base64);
	}

	public MessageLite leaveRatRequest(Event e) {
		this.thread.interrupt();
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.RAT_LEAVE)
				.setAttestationLeave(
						AttestationLeave
						.newBuilder()
						.setAtype(this.aType)
						.build()
						)
				.build();
	}
}
