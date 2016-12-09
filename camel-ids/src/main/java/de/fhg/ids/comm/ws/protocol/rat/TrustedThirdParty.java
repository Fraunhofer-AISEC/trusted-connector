package de.fhg.ids.comm.ws.protocol.rat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
	private PcrMessage msg = null;
	private URI adr;
	
	public PcrMessage getMsg() {
		return msg;
	}

	public void setMsg(PcrMessage msg) {
		this.msg = msg;
	}
	
	public TrustedThirdParty(URI adr) {
		LOG.debug("RAT REPOSITORY starting @ " + adr);
		this.adr = adr;
	}	

	public boolean pcrValuesCorrect(PcrValue[] values, String nonce) {
		if(values != null) {
			this.msg = new PcrMessage(values, nonce);
			PcrMessage response;
			try {
				response = this.readResponse(this.toJsonString(this.msg));
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
	
	public String toJsonString(PcrMessage msg) {
		Gson gson = new Gson();
		if(this.msg != null) {
			return gson.toJson(msg);	
		}
		else {
			return "";
		}
		
	}
	
	public PcrMessage readResponse(String json) throws IOException {
		Gson gson = new Gson();
		HttpURLConnection conn = (HttpURLConnection) this.adr.toURL().openConnection();
		LOG.debug("---------------------------------------connecting to : " + this.adr.toURL().toString());
		conn.setConnectTimeout(2500);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		//conn.setFixedLengthStreamingMode(json.getBytes("utf-8").length);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		InputStream is = conn.getInputStream();
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes("utf-8"));
		os.flush();
		os.close();
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
