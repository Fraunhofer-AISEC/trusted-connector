package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;

public class TrustedThirdParty {

	private Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private String ttpUrl = "http://127.0.0.1:7331";
	private String freshNonce = "";
	private ConnectorMessage msg = null;
	
	public TrustedThirdParty(ConnectorMessage msg) {
		this.msg = msg;
	}

	public TrustedThirdParty() {
		this.msg = null;
	}

	public boolean pcrValuesCorrect() throws IOException {
		freshNonce = NonceGenerator.generate();
		if(this.msg != null) {
			String json = this.jsonToString(freshNonce);
			PcrMessage response = this.readResponse(json, ttpUrl);
			LOG.debug(response.toString());
			return response.isSuccess();
		}
		else {
			return false;
		}
	}
	
	public String jsonToString(String nonce) throws IOException {
		Gson gson = new Gson();
		return gson.toJson(new PcrMessage(msg));
	}
	
	public PcrMessage readResponse(String json, String query) throws IOException {
		Gson gson = new Gson();
		URL url = new URL(query);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5000);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.toString().getBytes("utf-8"));
		os.flush();
		os.close();
		String jsonString = this.inputStreamToString(new BufferedInputStream(conn.getInputStream()));
		return gson.fromJson(jsonString, PcrMessage.class);
	}

	private  String inputStreamToString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().parallel().collect(Collectors.joining("\n"));
	}
}
