/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
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
package de.fhg.aisec.ids.dataflowcontrol

import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.idscp2.Idscp2UsageControlInterface
import de.fhg.aisec.ids.api.policy.PDP
import org.apache.camel.CamelContext
import org.apache.camel.NamedNode
import org.apache.camel.Processor
import org.apache.camel.spi.InterceptStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("idsCamelInterceptor")
class CamelInterceptor : InterceptStrategy {
    @Autowired(required = false)
    private var pdp: PDP? = null

    @Autowired(required = false)
    private var usageControlInterface: Idscp2UsageControlInterface? = null

    @Autowired(required = false)
    private var containerManager: ContainerManager? = null

    init {
        instance = this
    }

    override fun wrapProcessorInInterceptors(
        context: CamelContext,
        node: NamedNode,
        target: Processor,
        nextTarget: Processor?
    ): Processor = PolicyEnforcementPoint(node, target)

    companion object {
        private lateinit var instance: CamelInterceptor

        val pdp
            get() = instance.pdp
        val usageControlInterface
            get() = instance.usageControlInterface
        val containerManager
            get() = instance.containerManager
    }
}
