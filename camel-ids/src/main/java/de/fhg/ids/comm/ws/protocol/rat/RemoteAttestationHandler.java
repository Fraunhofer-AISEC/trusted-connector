package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;

public class RemoteAttestationHandler {
	
	protected String SOCKET = "tpm2sim/socket/control.sock";

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
	
	public static MessageLite sendError(Thread t, String code, String error) {
		if(t.isAlive()) {
			t.interrupt();
		}
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode(code)
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
