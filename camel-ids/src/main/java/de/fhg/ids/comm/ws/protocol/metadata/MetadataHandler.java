package de.fhg.ids.comm.ws.protocol.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.json.JSONArray;
//import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
//import de.fhg.ids.docker.Docker;

public class MetadataHandler {
	protected static Logger LOG = LoggerFactory.getLogger(MetadataHandler.class);
	protected static String lastError = "";
	protected long sessionID = -1;
	// all metadata keys i want to know from you
	protected List<String> myKeys = Arrays.asList("labels");
	// all metadata keys you want to know from me
	protected List<String> yourKeys = Arrays.asList();
	// all metadata values i provide to you
	protected List<String> myValues = new ArrayList<String>();
	// all metadata values you provide to me	
	protected List<String> yourValues = new ArrayList<String>();
	
	public List<String> generateMetaData(List<String> keys) {
		//Docker docker = new Docker();
		//docker.connectClient();
		List<String> values = new ArrayList<String>();
		for(String key: keys) {
			String meta;
			//JSONArray meta;
			switch(key) {
				case "labels":
					meta = "label desc";
					//meta = docker.getJsonLabels();
					break;
				default:
					meta = "error";
					//meta = new JSONArray("error: wrong key \""+key+"\" defined!");
					break;
			}
			//JSONObject answer = new JSONObject();
			//answer.put(key, meta);
			//values.add(answer.toString());
			values.add(meta);
		}
		return values;
	}
	
	public static MessageLite sendError(String lastError) {
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage(lastError)
						.build())
				.build();
	}
}