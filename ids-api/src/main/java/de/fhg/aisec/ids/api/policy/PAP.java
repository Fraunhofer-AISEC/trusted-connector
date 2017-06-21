package de.fhg.aisec.ids.api.policy;

import java.io.InputStream;
import java.util.List;

/**
 * Policy Administration Point Interface.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface PAP {
	
	/**
	 * Loads a policy into the registered PDPs.
	 * 
	 * @param is
	 */
	void loadPolicy(InputStream is);
	
	/**
	 * Returns the currently active policy in its string representation.
	 * 
	 * The representation depends on the implementation and can be XML, JSON or any other serialization.
	 * 
	 * @return
	 */
	String getPolicy();
	
	List<String> listRules();
}
