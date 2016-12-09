package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.PcrValue;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.attestation.PcrMessage;

public class TrustedThirdParty {

	private Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private URI adr;
	private HttpURLConnection conn;

	public TrustedThirdParty(URI adr) {
		this.adr = adr;
	}	

	public boolean pcrValuesCorrect(PcrValue[] values, String nonce) {
		if(values != null) {
			try {
				PcrMessage response = this.readResponse(new PcrMessage(values, nonce));
				return response.isSuccess();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	public String toJsonString(PcrMessage msg) {
		Gson gson = new Gson();
		return gson.toJson(msg);
	}
	
	public PcrMessage readResponse(PcrMessage msg) throws IOException {
		try {
			conn = (HttpURLConnection) this.adr.toURL().openConnection();
		} catch (MalformedURLException e) {
			LOG.debug("TrustedThirdParty: MalformedURLException ! " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			LOG.debug("TrustedThirdParty: IOException ! " + e.getMessage());
			e.printStackTrace();
		}
		LOG.debug("TrustedThirdParty connected to: " + this.adr.toURL());		
		Gson gson = new Gson();
		LOG.debug("sending: " + gson.toJson(msg));
		conn.setConnectTimeout(5000);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setRequestProperty("Accept", "application/json");
		//conn.setFixedLengthStreamingMode(json.getBytes("utf-8").length);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
		out.write(gson.toJson(msg));
		out.close();
		InputStream is = conn.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		String jsonString = this.inputStreamToString(bis);
		return gson.fromJson(jsonString, PcrMessage.class);
	}

	private  String inputStreamToString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().parallel().collect(Collectors.joining("\n"));
	}

	public static PcrValue[] fill(Pcr[] array) {
		PcrValue[] converted = new PcrValue[array.length];
		for(int i = 0; i < converted.length; ++i) {
			converted[i] = new PcrValue(array[i].getNumber(), array[i].getValue());
		}
		return converted;
	}
}
