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
 * Policy Decision Point (PDP) Interface.
 *
 *
 * The PDP decides decision requests against a policy. It may use caching to speed up the
 * decision.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
interface PDP {
    /**
     * Main method for requesting a policy decision.
     *
     *
     * The decision request states attributes of subject and resource. The result is a decision
     * that is expected to be enforced by the PEP.
     *
     * @param req The decision request, wrapping relevant data for policy decision
     * @return The policy decision
     */
    fun requestDecision(req: DecisionRequest): PolicyDecision

    /** Removes all data from PDP-internal caches. Future decisions will possibly take more time.  */
    fun clearAllCaches()

    /**
     * Requests the PDP for the result of applying a transformation function to a message.
     *
     *
     * Transformation functions remove and/or add labels to messages. For non-flow-aware services,
     * transformation functions are defined as part of the policy.
     *
     *
     * A transformation function must always applied to a message before the policy decision is
     * requested using `requestDecision`.
     *
     * @param lastServiceNode The last Processor the message exchange has processed
     * @return The label transformations to apply to the Exchange
     */
    fun requestTranformations(lastServiceNode: ServiceNode): TransformationDecision
}
