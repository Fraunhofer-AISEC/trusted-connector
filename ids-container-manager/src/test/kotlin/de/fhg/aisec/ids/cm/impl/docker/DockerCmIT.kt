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
package de.fhg.aisec.ids.cm.impl.docker

import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerStatus
import de.fhg.aisec.ids.cm.impl.docker.DockerCM.Companion.isSupported
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.util.*

class DockerCmIT {
    private val wipes: MutableSet<String> = HashSet()
    @After
    fun cleanUp() {
        // Remove containers created during test
        DockerCM().let { d -> wipes.forEach { d.wipe(it) } }
    }

    @Test
    fun testList() {
        Assume.assumeTrue(isSupported)
        val d = DockerCM()

        // List running containers
        val lRunning = d.list(true)
        Assert.assertNotNull(lRunning)

        // List all containers (also stopped ones)
        val lAll = d.list(false)
        Assert.assertNotNull(lAll)

        // we cannot have less running than total containers
        Assert.assertTrue(lAll.size >= lRunning.size)
    }

    @Test
    fun testPull() {
        Assume.assumeTrue(isSupported)
        val d = DockerCM()

        // Pull the smallest possible image. Blocks. (must be online)
        val app = ApplicationContainer()
        app.image = "tianon/true"
        val oContainerID = d.pullImage(app)

        // We expect a new container to be created
        Assert.assertTrue(oContainerID.isPresent)
        Assert.assertNotEquals("", oContainerID.get())
        wipes.add(oContainerID.get())

        // we expect the container to be in list()
        val container = d.list(false).firstOrNull { it.id == oContainerID.get() }
        Assert.assertNotNull(container)
        Assert.assertEquals(ContainerStatus.CREATED, container?.status)
    }

    @Test
    fun testVersion() {
        Assume.assumeTrue(isSupported)
        val d = DockerCM()
        val version = d.version
        Assert.assertFalse(version.isEmpty())
        val regex = Regex(".* \\([0-9.]+(?:.+)?\\)")
        if (!version.matches(regex)) {
            throw AssertionError(
                    "Error: Docker version has to match regex '$regex', found '$version'")
        }
    }

    @Test
    fun testStartStop() {
        Assume.assumeTrue(isSupported)
        val d = DockerCM()

        // Pull an image we can actually start. (must be online)
        val app = ApplicationContainer()
        app.image = "nginx"
        val oContainerID = d.pullImage(app)

        // We expect a new container to be created
        Assert.assertTrue(oContainerID.isPresent)
        val containerID = oContainerID.get()
        wipes.add(containerID)
        Assert.assertNotNull(containerID)

        // we expect the container to be in list()
        val container = d.list(false).firstOrNull { it.id == containerID }
        Assert.assertNotNull(container)
        Assert.assertEquals(ContainerStatus.CREATED, container?.status)

        // Start container
        d.startContainer(containerID, null)

        // We now expect it in list of running containers
        val runningContainer = d.list(true).firstOrNull { it.id == containerID }
        Assert.assertNotNull(runningContainer)
        Assert.assertEquals(ContainerStatus.RUNNING, runningContainer?.status)

        // Stop container
        d.stopContainer(containerID)

        // We expect it to be still in list of all containers
        val stoppedContainer = d.list(false).firstOrNull { it.id == containerID }
        Assert.assertNotNull(stoppedContainer)
        Assert.assertEquals(ContainerStatus.EXITED, stoppedContainer?.status)
    }
}