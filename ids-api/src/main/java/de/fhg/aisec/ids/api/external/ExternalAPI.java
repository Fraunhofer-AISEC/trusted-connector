package de.fhg.aisec.ids.api.external;

import java.nio.channels.Channel;
import java.util.concurrent.CompletableFuture;

import de.fhg.aisec.ids.messages.BrokerProtos.AnnounceServiceResponse;
import de.fhg.aisec.ids.messages.BrokerProtos.ServiceDescription;
import de.fhg.aisec.ids.messages.ConnectorProtos.AttestationResponse;

/**
 * Interface of an IDS connector towards the IDS network, i.e. outside of the
 * connector itself.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de), Gerd Brost
 *
 */
public interface ExternalAPI {

	public void addEndpoint(String s, Channel channel);
	
	public boolean hasEndpoint(String s);

	/**
	 * Method to announce a service to a broker
	 * @param description Description of the service to be accounced
	 * @return Service accouncement response 
	 */
	CompletableFuture<AnnounceServiceResponse> announceService(ServiceDescription description);
	
	/**
	 * Method to request attestation
	 * @param URI URI of the connecter that is to be verified
	 * @param nonce Nonce for freshness proof
	 * @return Returns the response of the attestation process
	 */
	CompletableFuture<AttestationResponse> requestAttestation(String URI, int nonce);

}
