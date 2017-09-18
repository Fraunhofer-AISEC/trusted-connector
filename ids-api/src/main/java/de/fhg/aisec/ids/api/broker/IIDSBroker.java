/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform API
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
