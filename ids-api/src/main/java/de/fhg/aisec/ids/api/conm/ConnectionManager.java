package de.fhg.aisec.ids.api.conm;

import java.util.List;



/**
 * List and interacts with open connections over the IDS communication protocol
 * 
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
public interface ConnectionManager {
	
	/**
	 * List currently installed connections.
	 * 

	 */
	public List<IDSCPConnection> listConnections();
}
