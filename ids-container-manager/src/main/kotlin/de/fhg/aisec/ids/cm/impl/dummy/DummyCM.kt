/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.aisec.ids.cm.impl.dummy

import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.cm.Decision
import de.fhg.aisec.ids.api.cm.Direction
import de.fhg.aisec.ids.api.cm.Protocol

/**
 * Dummy implementation of a null container manager which is used if no real CMLd is available.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
class DummyCM : ContainerManager {
    override fun list(onlyRunning: Boolean): List<ApplicationContainer> {
        return ArrayList()
    }

    override fun wipe(containerID: String) {}
    override fun startContainer(containerID: String, key: String?) {}
    override fun stopContainer(containerID: String) {}
    override fun restartContainer(containerID: String) {}
    override fun pullImage(app: ApplicationContainer): String? {
        return null
    }

    override fun getMetadata(containerID: String): Map<String, String> {
        return HashMap()
    }

    override fun setIpRule(
        containerID: String,
        direction: Direction,
        srcPort: Int,
        dstPort: Int,
        srcDstRange: String,
        protocol: Protocol,
        decision: Decision
    ) {
    }

    override fun inspectContainer(containerID: String): String {
        return ""
    }

    override val version: String
        get() = "no cmld installed"
}
