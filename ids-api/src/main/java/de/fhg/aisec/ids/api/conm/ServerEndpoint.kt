/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.api.conm

/**
 * Bean representing an "Endpoint".
 * This maps to a camel endpoint and is used to represent exposed endpoints.
 *
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
data class ServerEndpoint(
    var endpointIdentifier: String,
    var defaultProtocol: String,
    var host: String,
    var port: String
)
