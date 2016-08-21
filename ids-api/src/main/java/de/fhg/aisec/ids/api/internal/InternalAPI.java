package de.fhg.aisec.ids.api.internal;

import java.rmi.Remote;

/**
 * Internal interface of the IDS core platform, i.e. methods the IDS Core
 * Platform provides to other containers within the same connector.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface InternalAPI extends Remote {

	/**
	 * Returns an array of broker endpoints used by Communication Manager.
	 * 
	 * @return
	 */
	public String[] getBrokerEndpoints();
}
