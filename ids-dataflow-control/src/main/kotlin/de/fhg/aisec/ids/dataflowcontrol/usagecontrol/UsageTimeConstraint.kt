/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
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
package de.fhg.aisec.ids.dataflowcontrol.usagecontrol

import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.Constraint
import javax.xml.datatype.DatatypeFactory

class UsageTimeConstraint(private val constraint: Constraint) : LuconConstraint {
    private var notAfterDateTime: String? = null
    private var notBeforeDateTime: String? = null

    private fun checkSystemConstraint(
        context: EnforcementContext,
        permission: LuconPermission,
        policyDescription: () -> String
    ) {
        if (!permission.constraints.any { it is DockerImageConstraint }) {
            context.log.warn(
                "This UC constraint (${policyDescription.invoke()}) cannot be reliably enforced without an " +
                    "accompanying SYSTEM constraint (e.g. Docker image constraint)!"
            )
        }
    }

    override fun checkEnforcible(
        context: EnforcementContext,
        permission: LuconPermission
    ) {
        val rightOperand = constraint.rightOperand
        if (rightOperand.type != TYPE_DATETIMESTAMP) {
            throw LuconException("Unexpected RDF resource type ${rightOperand.type}, $TYPE_DATETIMESTAMP expected")
        }
        val currentTimestamp = System.currentTimeMillis()
        val policyTimestamp =
            DATATYPE_FACTORY.newXMLGregorianCalendar(rightOperand.value).toGregorianCalendar()
                .timeInMillis
        if (constraint.operator == BinaryOperator.BEFORE) {
            if (currentTimestamp < policyTimestamp) {
                checkSystemConstraint(context, permission) { "No usage after ${rightOperand.value}" }
                notAfterDateTime = rightOperand.value
            } else {
                throw LuconException("Permitted usage period (until ${rightOperand.value}) has already elapsed!")
            }
        } else if (constraint.operator == BinaryOperator.AFTER) {
            if (currentTimestamp >= policyTimestamp) {
                if (context.log.isDebugEnabled) {
                    context.log.debug(
                        "The usage start constraint (not before ${rightOperand.value}) is already fulfilled!"
                    )
                }
            } else {
                checkSystemConstraint(context, permission) { "No usage before ${rightOperand.value}" }
            }
            notBeforeDateTime = rightOperand.value
        }
    }

    override fun enforce(context: EnforcementContext) {
        notAfterDateTime?.let { context.ucPolicies.put("notAfterDateTime", it) }
        notBeforeDateTime?.let { context.ucPolicies.put("notBeforeDateTime", it) }
    }

    companion object {
        const val TYPE_DATETIMESTAMP = "http://www.w3.org/2001/XMLSchema#dateTimeStamp"
        val DATATYPE_FACTORY: DatatypeFactory = DatatypeFactory.newInstance()
    }
}
