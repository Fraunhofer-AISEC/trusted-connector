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

import de.fhg.aisec.ids.messages.Idscp.Pcr;

public class TrustedThirdParty {

	private Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private String ttpUrl = "http://localhost:7331";
	private String freshNonce = "";
	private Pcr[] pcrValues = null;
	
	public TrustedThirdParty(Pcr[] pcrValues) {
		this.pcrValues = pcrValues;
	}

	public TrustedThirdParty() {
		this.pcrValues = null;
	}

	public boolean pcrValuesCorrect() throws IOException {
		freshNonce = NonceGenerator.generate();
		if(this.pcrValues != null) {
			JsonObject response = this.readResponse(this.jsonToString(freshNonce), ttpUrl);
			
			return true;
		}
		else {
			return false;
		}
	}
	
	public String jsonToString(String nonce) throws IOException {
		Gson gson = new Gson();
		return gson.toJson(new PcrMessage(nonce, pcrValues));
	}
	
	public JsonObject readResponse(String json, String query) throws IOException {
		URL url = new URL(query);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5000);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.toString().getBytes("utf-8"));
		os.close();
		// read the response
		String result = this.inputStreamToString(new BufferedInputStream(conn.getInputStream()));
		JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
		conn.disconnect();
		return jsonObject;
    }

	private  String inputStreamToString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().parallel().collect(Collectors.joining("\n"));
	}
}
