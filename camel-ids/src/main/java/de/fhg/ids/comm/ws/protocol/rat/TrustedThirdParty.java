package de.fhg.ids.comm.ws.protocol.rat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.fhg.aisec.ids.messages.Idscp.Pcr;

public class TrustedThirdParty {

	private static Logger LOG = LoggerFactory.getLogger(TrustedThirdParty.class);
	
	public static boolean pcrValuesCorrect(Pcr[] pcrValues) {
		JsonObject values = new JsonObject();
		TrustedThirdParty.add(values, "pcrValues", pcrValues);
		LOG.debug("------------------------------------------------------------------------------------ JSON:" + values.toString());
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
