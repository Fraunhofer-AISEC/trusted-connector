package de.fhg.aisec.ids.api.internal;

import de.fhg.aisec.ids.messages.ConnectorProtos.ServiceRequest;
import de.fhg.aisec.ids.messages.ConnectorProtos.ServiceResponse;

/**
 * Listener interface for notifications on data requests to a service.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface ServiceRequestListener {

	/**
	 * Callback method to be called when a service is invoked.
	 * 
	 * Implementations are expected to handle the request and return a
	 * <code>ServiceResponse</code> within an acceptable time (i.e., some
	 * seconds, depending on the timeout accepted by client).
	 * 
	 * @param req
	 * @return
	 */
	public ServiceResponse onServiceRequest(ServiceRequest req);
}
