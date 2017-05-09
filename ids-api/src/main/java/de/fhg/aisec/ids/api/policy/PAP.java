package de.fhg.aisec.ids.api.policy;

import java.io.InputStream;

/**
 * Policy Administration Point Interface.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface PAP {
	
	public void loadPolicy(InputStream is);
	
	public String getPolicy();
}
