package de.fhg.aisec.ids.api.internal;

/**
 * Listener interface for notifications on failed service registration attempts.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de
 *
 */
public interface RegistrationFailedListener {
	
	/**
	 * Callback method to be called when a service registration has failed.
	 * 
	 */
	public void onRegistrationFailed(String errorCode, String errorMessage);
}
