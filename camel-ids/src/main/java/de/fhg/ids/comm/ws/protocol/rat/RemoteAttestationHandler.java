/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RemoteAttestationHandler {
	
	protected static Logger LOG = LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
	protected static String lastError = "";
	// used to count messages between ids connector and attestation repository
	protected static long privateID = new java.util.Random().nextLong();  

	public static boolean checkRepository(IdsAttestationType basic, AttestationResponse response, URI ttpUri, SSLContextParameters params) {
		int numPcrValues = response.getPcrValuesCount();
		String nonce = response.getQualifyingData();
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
			        		.setAtype(basic)
			        		.setQualifyingData(nonce)
			        		.addAllPcrValues(Arrays.asList(values))
			        		.build()
							)
					.build(), 
					ttpUri.toURL(),
					params);
			
			LOG.debug("//Q///////////////////////////////////////////////////////////////////////////");
			LOG.debug(response.toString());
			LOG.debug("//A///////////////////////////////////////////////////////////////////////////");
			LOG.debug(msgRepo.toString());
			LOG.debug("/////////////////////////////////////////////////////////////////////////////");
			
			return (
					msgRepo.getAttestationRepositoryResponse().getResult() 
					&& (msgRepo.getId() == privateID + 1) 
					&& (msgRepo.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE))
					&& (msgRepo.getAttestationRepositoryResponse().getQualifyingData().equals(nonce))
					&& true); // TODO : signature check of repo answer ... !
			
		} catch (Exception ex) {
			lastError = "Exception:" + ex.getMessage();
			LOG.debug("//Exception///////////////////////////////////////////////////////////////////////////");
			LOG.debug(lastError);
			LOG.debug("//Exception///////////////////////////////////////////////////////////////////////////");			
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
					LOG.warn(lastError, ex);
					return false;
				}
			} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
				lastError = "error: could not convert from TPM2B_PUBLIC (\""+tpm2bPublickey+"\") to a PublicKey :" + ex.getMessage();
				LOG.warn(lastError, ex);
				return false;
			}
		} catch (Exception ex) {
			lastError = "error: could not create a TPM2B_PUBLIC (\""+ByteArrayUtil.toPrintableHexString(byteSignature)+"\") :" + ex.getMessage();
			LOG.warn(lastError, ex);
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
	
	public static ConnectorMessage readRepositoryResponse(ConnectorMessage msg, URL adr, SSLContextParameters params) throws IOException, NoSuchAlgorithmException, GeneralSecurityException, KeyManagementException {
        
		// Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
		
		HttpsURLConnection urlc = (HttpsURLConnection) adr.openConnection();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(params.getKeyManagers().createKeyManagers(), trustAllCerts, new SecureRandom());
        urlc.setSSLSocketFactory(sslContext.getSocketFactory());
		urlc.setUseCaches(false);
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        urlc.setRequestProperty("User-Agent","IDS-Connector");
        urlc.setRequestProperty("Content-length",String.valueOf(msg.toByteArray().length));
        msg.writeTo(urlc.getOutputStream());
        return ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
	}
}
