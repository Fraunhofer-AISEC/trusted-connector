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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.fhg.aisec.ids.messages.Idscp.Pcr;

public class TrustedThirdParty {

	private static Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private static String ttpUrl = "http://localhost:7331";
	
	public static boolean pcrValuesCorrect(Pcr[] pcrValues) throws IOException {
		JsonObject values = new JsonObject();
		TrustedThirdParty.add(values, "pcrValues", pcrValues);
		return TrustedThirdParty.check(values, ttpUrl);
	}
	
	private static boolean check(JsonObject values, String url) throws IOException {
		JsonObject result = TrustedThirdParty.readResponse(values, url);
		
		return true;
	}

	public static void add(JsonObject jo, String property, Pcr[] values) {
	    JsonArray array = new JsonArray();
	    for (Pcr value : values) {
	    	JsonArray item = new JsonArray();
	    	item.add(value.getNumber());
	    	item.add(value.getValue());
	        array.add(item);
	    }
	    jo.add(property, array);
	}
	
	private static JsonObject readResponse(JsonObject json, String query) throws IOException {
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
            String result = TrustedThirdParty.inputStreamToString(new BufferedInputStream(conn.getInputStream()));
            JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
            conn.disconnect();
            return jsonObject;
    }

	private static String inputStreamToString(InputStream in) {
		return new BufferedReader(new InputStreamReader(in)).lines().parallel().collect(Collectors.joining("\n"));
	}

}
