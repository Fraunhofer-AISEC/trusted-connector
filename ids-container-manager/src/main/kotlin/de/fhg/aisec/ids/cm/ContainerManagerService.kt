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
package de.fhg.aisec.ids.cm

import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.cm.Decision
import de.fhg.aisec.ids.api.cm.Direction
import de.fhg.aisec.ids.api.cm.NoContainerExistsException
import de.fhg.aisec.ids.api.cm.Protocol
import de.fhg.aisec.ids.cm.impl.docker.DockerCM
import de.fhg.aisec.ids.cm.impl.docker.DockerCM.Companion.isSupported
import de.fhg.aisec.ids.cm.impl.dummy.DummyCM
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Main entry point of the Container Management Layer.
 *
 *
 * This class is mainly a facade for the actual CML implementation, which can either be Docker or
 * trust-X.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
@Component("idsContainerManager")
class ContainerManagerService : ContainerManager {
    private val containerManager: ContainerManager
    private val defaultCM: ContainerManager
        get() {
            return when {
                TrustXCM.isSupported -> {
                    TrustXCM()
                }
                isSupported -> {
                    DockerCM()
                }
                else -> {
                    LOG.warn("No supported container management layer found. Using dummy")
                    DummyCM()
                }
            }
        }

    override fun list(onlyRunning: Boolean): List<ApplicationContainer> {
        return containerManager.list(onlyRunning)
    }

    override fun wipe(containerID: String) {
        try {
            containerManager.wipe(containerID)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
    }

    override fun startContainer(containerID: String, key: String?) {
        try {
            containerManager.startContainer(containerID, key)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
    }

    override fun stopContainer(containerID: String) {
        try {
            containerManager.stopContainer(containerID)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
    }

    override fun restartContainer(containerID: String) {
        try {
            containerManager.restartContainer(containerID)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
    }

    override fun pullImage(app: ApplicationContainer): String? {
        try {
            return containerManager.pullImage(app)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
        return null
    }

    override fun inspectContainer(containerID: String): String? {
        try {
            return containerManager.inspectContainer(containerID)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
        return ""
    }

    override fun getMetadata(containerID: String): Any? {
        try {
            return containerManager.getMetadata(containerID)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
        return null
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
        containerManager.setIpRule(
            containerID, direction, srcPort, dstPort, srcDstRange, protocol, decision
        )
    }

    override val version: String
        get() = containerManager.version

    companion object {
        private val LOG = LoggerFactory.getLogger(ContainerManagerService::class.java)
    }

    init {
        // When activated, try to set container management instance
        containerManager = defaultCM
        LOG.info("Default container management is {}", containerManager)
    }
}
