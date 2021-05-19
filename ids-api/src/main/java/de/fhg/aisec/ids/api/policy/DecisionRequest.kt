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
package de.fhg.aisec.ids.api.policy

/**
 * Data structure holding a decision request which is sent to the PDP. The PDP is expected to answer
 * with a PolicyDecision object.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
data class DecisionRequest(
    /** The processor that data is received from  */
    val from: ServiceNode,
    /** The Processor that the data is to be sent to  */
    val to: ServiceNode,
    /** Properties of the message (e.g., labels)  */
    val labels: Set<String>,
    /** Properties of the environment  */
    val environmentCtx: Map<String, Any>?
)
