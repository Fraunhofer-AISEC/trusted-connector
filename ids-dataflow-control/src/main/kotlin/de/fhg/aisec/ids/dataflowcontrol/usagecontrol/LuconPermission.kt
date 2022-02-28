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

import de.fraunhofer.iais.eis.Action
import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.Constraint
import de.fraunhofer.iais.eis.LeftOperand
import de.fraunhofer.iais.eis.Permission

class LuconPermission(permission: Permission) {
    val constraints: List<LuconConstraint>

    init {
        if (permission.action.size != 1 || permission.action[0] != Action.USE) {
            throw LuconException("Only permissions with a single Action USE are supported yet!")
        }
        if (permission.preDuty?.isNotEmpty() == true || permission.postDuty?.isNotEmpty() == true) {
            throw LuconException("PreDuties and PostDuties are not supported yet!")
        }
        constraints = permission.constraint.map { constraint ->
            if (constraint !is Constraint) {
                throw LuconException("Encountered constraint of invalid type ${constraint.javaClass.name}")
            }
            when (constraint.leftOperand) {
                LeftOperand.SYSTEM ->
                    when (constraint.operator) {
                        BinaryOperator.SAME_AS -> DockerImageConstraint(constraint.rightOperandReference)
                        else -> throw LuconException(
                            "Unexpected Operator ${constraint.operator} for LeftOperand ${constraint.leftOperand}"
                        )
                    }
                LeftOperand.POLICY_EVALUATION_TIME ->
                    when (constraint.operator) {
                        BinaryOperator.BEFORE, BinaryOperator.AFTER -> UsageTimeConstraint(constraint)
                        else -> throw LuconException(
                            "Unexpected Operator ${constraint.operator} for LeftOperand ${constraint.leftOperand}"
                        )
                    }
                else -> throw LuconException("Unexpected LeftOperand ${constraint.leftOperand}")
            }
        }
    }

    fun checkEnforcible(ectx: EnforcementContext) {
        constraints.withIndex().forEach { (i, c) ->
            if (ectx.log.isDebugEnabled) {
                ectx.log.debug("Checking constraint # ${i + 1}...")
            }
            c.checkEnforcible(ectx, this)
        }
    }

    fun enforce(ectx: EnforcementContext) {
        constraints.forEach { it.enforce(ectx) }
    }
}
