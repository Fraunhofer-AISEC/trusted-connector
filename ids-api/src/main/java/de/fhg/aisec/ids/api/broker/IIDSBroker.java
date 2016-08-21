package de.fhg.aisec.ids.api.broker;

import java.util.Set;

import de.fhg.aisec.ids.messages.BrokerProtos.ServiceDescription;

/**
 * Interface of the IDS Broker.
 * 
 * The broker is managing a directory of services which are currently available
 * in the IDS infrastructure. Note that information provided by the broker is
 * not necessarily current or correct, i.e. service endpoints might not be
 * available or not comply with the service description provided by the Broker:
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface IIDSBroker {

	/**
	 * Announce that a service is available over the IDS infrastructure.
	 * 
	 * @param description Description of the service.
	 */
	public void registerService(ServiceDescription description);

	/**
	 * Return a list of all currently registered services.
	 * @return
	 */
	public Set<ServiceDescription> getServices();
}
