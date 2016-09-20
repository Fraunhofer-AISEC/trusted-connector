package de.fhg.aisec.ids.api;

import java.util.Map;

public interface MetaDataInfoPoint {

	/**
	 * Returns a key/value set for an internal app.
	 * 
	 * @param appId
	 * @return
	 */
	public Map<String, String> getContainerLabels(String containerID);
}
