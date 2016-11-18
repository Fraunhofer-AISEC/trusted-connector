package de.fhg.ids.comm.ws.protocol.rat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.fhg.aisec.ids.messages.Idscp.Pcr;

public class TrustedThirdParty {

	private static Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	private static String ttpUrl = "http://localhost:7331";
	
	public static boolean pcrValuesCorrect(Pcr[] pcrValues) {
		JsonObject values = new JsonObject();
		TrustedThirdParty.add(values, "pcrValues", pcrValues);
		return TrustedThirdParty.check(values, ttpUrl);
	}
	
	private static boolean check(JsonObject values, String string) {
		// TODO Auto-generated method stub
		return false;
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

}
