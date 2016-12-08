package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.fhg.aisec.ids.attestation.PcrMessage;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

public class TrustedThirdParty {

	private Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private String freshNonce = "";
	private ConnectorMessage msg = null;
	private URI adr;
	
	public TrustedThirdParty(ConnectorMessage msg, URI adr) {
		this.msg = msg;
		this.adr = adr;
	}

	public TrustedThirdParty(URI adr) {
		this.msg = null;
		this.adr = adr;
	}

	public boolean pcrValuesCorrect() {
		freshNonce = NonceGenerator.generate();
		if(this.msg != null) {
			String json = this.jsonToString(freshNonce);
			PcrMessage response;
			try {
				response = this.readResponse(json);
				LOG.debug(response.toString());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return response.isSuccess();
		}
		else {
			return false;
		}
	}
	
	public String jsonToString(String nonce) {
		Gson gson = new Gson();
		return gson.toJson(new PcrMessage(msg));
	}
	
	public PcrMessage readResponse(String json) throws IOException {
		Gson gson = new Gson();
		HttpURLConnection conn = (HttpURLConnection) this.adr.toURL().openConnection();
		conn.setConnectTimeout(5000);
		conn.setRequestProperty("Cache-Control", "no-cache");
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes("utf-8"));
		os.flush();
		os.close();
		InputStream is = conn.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		String jsonString = this.inputStreamToString(bis);
		return gson.fromJson(jsonString, PcrMessage.class);
	}

	private  String inputStreamToString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().parallel().collect(Collectors.joining("\n"));
	}
}
