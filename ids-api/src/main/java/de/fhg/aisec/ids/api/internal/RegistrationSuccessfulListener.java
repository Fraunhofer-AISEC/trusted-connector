package de.fhg.aisec.ids.api.internal;

/**
 * Listener interface for notifications on failed service registration attempts.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface RegistrationSuccessfulListener {
	
	/**
	 * Callback method to be called when a service registration has succeeded.
	 * 
	 * TODO Provide information about the registered service (IDS-42).
	 */
	public void onRegistrationSuccessful();
}
