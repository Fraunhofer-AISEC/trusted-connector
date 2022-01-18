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
package de.fhg.aisec.ids.api.router

import java.lang.NullPointerException
import java.lang.StringBuilder
import java.util.ArrayList

/**
 * Representation of a proof that a route is valid under a policy, i.e. that the policy will never
 * violate the policy.
 *
 *
 * If the route can violate the policy, a set of counterExamples is given.
 *
 *
 * The set is not necessarily complete and contains message paths which are valid in term of the
 * route, but violate the policy.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
class RouteVerificationProof(routeId: String?) {
    val routeId: String
    var proofTimeNanos: Long = 0
    var isValid = true
    var counterExamples: List<CounterExample> = ArrayList()
    var query = ""

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Proof for ").append(query).append(" is ").append(if (isValid) "VALID" else "INVALID").append("\n")
            .append("Example flows violating policy:\n")
        for (ce in counterExamples) {
            sb.append("|-- ").append(ce.toString()).append("\n\n")
        }
        return sb.toString()
    }

    init {
        if (routeId == null) {
            throw NullPointerException("routeId must not be null")
        }
        this.routeId = routeId
    }
}
