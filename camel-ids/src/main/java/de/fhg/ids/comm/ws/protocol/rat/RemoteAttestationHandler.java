package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RemoteAttestationHandler {
	
	protected static Logger LOG = LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
	protected static String lastError = "";
	// used to count messages between ids connector and attestation repository
	protected static long privateID = new java.util.Random().nextLong();
	
	/*
	// fetch a public key from a uri and return the key as a byte array
	protected static byte[] fetchPublicKey(String uri) throws Exception {
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
	*/
	
	public static boolean checkRepository(IdsAttestationType basic, String nonce, AttestationResponse response, URI ttpUri) {
		int numPcrValues = response.getPcrValuesCount();
		Pcr[] values = response.getPcrValuesList().toArray(new Pcr[numPcrValues]);
		try {
			ConnectorMessage msgRepo = RemoteAttestationHandler.readRepositoryResponse(
					ConnectorMessage
					.newBuilder()
					.setId(privateID)
					.setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
					.setAttestationRepositoryRequest(
							AttestationRepositoryRequest
			        		.newBuilder()
			        		.setAtype(IdsAttestationType.BASIC)
			        		.setQualifyingData(nonce)
			        		.addAllPcrValues(Arrays.asList(values))
			        		.build()
							)
					.build()
					, ttpUri.toURL());
			
			return (
					msgRepo.getAttestationRepositoryResponse().getResult() 
					&& (msgRepo.getId() == privateID + 1) 
					&& (msgRepo.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE))
					&& (msgRepo.getAttestationRepositoryResponse().getQualifyingData().equals(nonce))
					&& true); // TODO : signature check of repo answer ... !
			
		} catch (MalformedURLException e1) {
			lastError = "MalformedURLException:" + e1.getMessage();
			return false;
		} catch (IOException e2) {
			lastError = "IOException:" + e2.getMessage();
			return false;
		}
	}
	
	public boolean checkSignature(AttestationResponse response, String nonce) {
		Signature sig;
		byte[] byteSignature = DatatypeConverter.parseHexBinary(response.getSignature());
		LOG.debug("signature:" + ByteArrayUtil.toPrintableHexString(byteSignature));
		byte[] byteCert = DatatypeConverter.parseHexBinary(response.getCertificateUri());
		LOG.debug("cert:" + ByteArrayUtil.toPrintableHexString(byteCert));
		byte[] byteQuoted = DatatypeConverter.parseHexBinary(response.getQuoted());
		try {
			// construct a new TPM2B_PUBLIC from byteCert bytes
			TPM2B_PUBLIC tpm2bPublickey = new TPM2B_PUBLIC(byteCert);
			try {
				// and convert it into a java DER PublicKey key
				PublicKey publicKey = new PublicKeyConverter(tpm2bPublickey).getPublicKey();
				try {
					// construct a new TPMT_SIGNATURE from byteSignature bytes
					TPMT_SIGNATURE tpmtSignature = new TPMT_SIGNATURE(byteSignature);
					// safe computation time and do NOT generate TPMS_ATTEST ..... just check bytes of TPMS_ATTEST in switch/case
					// TPMS_ATTEST tpmsAttest = new TPMS_ATTEST(byteQuoted);
					
					// check if nonces match
					boolean nonceCorrect = nonce.equals(response.getQualifyingData());

					// check signature depending on HashAlg() 
					switch(tpmtSignature.getSignature().getHashAlg()) {
						case TPM_ALG_SHA256:
							sig = Signature.getInstance("SHA256withRSA");
						    sig.initVerify(publicKey);
						    sig.update(byteQuoted);
						    return nonceCorrect && sig.verify(tpmtSignature.getSignature().getSig());
						case TPM_ALG_SHA1:
							sig = Signature.getInstance("SHA1withRSA");
						    sig.initVerify(publicKey);
						    sig.update(byteQuoted);
						    return nonceCorrect && sig.verify(tpmtSignature.getSignature().getSig());				
						default:
							return false;
					}
				} catch (Exception ex) {
					lastError = "error: could not create a TPMT_SIGNATURE from bytes \""+ByteArrayUtil.toPrintableHexString(byteSignature)+"\":" + ex.getMessage();
					LOG.debug(lastError);
					ex.printStackTrace();
					return false;
				}
			} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
				lastError = "error: could not convert from TPM2B_PUBLIC (\""+tpm2bPublickey+"\") to a PublicKey :" + ex.getMessage();
				LOG.debug(lastError);
				ex.printStackTrace();
				return false;
			}
		} catch (Exception ex) {
			lastError = "error: could not create a TPM2B_PUBLIC (\""+ByteArrayUtil.toPrintableHexString(byteSignature)+"\") :" + ex.getMessage();
			LOG.debug(lastError);
			ex.printStackTrace();
			return false;
		}
	}
	
	public static MessageLite sendError(Thread t, long id, String error) {
		if(t.isAlive()) {
			t.interrupt();
		}
		return ConnectorMessage
				.newBuilder()
				.setId(id)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage(error)
						.build())
				.build();
	}
	
	public static ConnectorMessage readRepositoryResponse(ConnectorMessage msg, URL adr) throws IOException {
        HttpURLConnection urlc = (HttpURLConnection) adr.openConnection();
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        msg.writeTo(urlc.getOutputStream());
        return ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
	}
}
