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
package de.fhg.aisec.ids.cm.impl.docker;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerStatus;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class DockerCmIT {
  private List<String> wipes = new ArrayList<>();

  @After
  public void cleanUp() {
    // Remove containers created during test
    DockerCM d = new DockerCM();
    wipes.forEach(d::wipe);
  }

  @Test
  public void testList() {
    assumeTrue(DockerCM.Companion.isSupported());

    DockerCM d = new DockerCM();

    // List running containers
    List<ApplicationContainer> lRunning = d.list(true);
    assertNotNull(lRunning);

    // List all containers (also stopped ones)
    List<ApplicationContainer> lAll = d.list(false);
    assertNotNull(lAll);

    // we cannot have less running than total containers
    assertTrue(lAll.size() >= lRunning.size());
  }

  @Test
  public void testPull() {
    assumeTrue(DockerCM.Companion.isSupported());

    DockerCM d = new DockerCM();

    // Pull the smallest possible image. Blocks. (must be online)
    ApplicationContainer app = new ApplicationContainer();
    app.setImage("tianon/true");
    Optional<String> oContainerID = d.pullImage(app);

    // We expect a new container to be created
    assertTrue(oContainerID.isPresent());
    assertNotEquals("", oContainerID.get());
    wipes.add(oContainerID.get());

    // we expect the container to be in list()
    List<ApplicationContainer> containers = d.list(false);
    Optional<ApplicationContainer> container =
        containers.stream().filter(c -> c.getId().equals(oContainerID.get())).findAny();
    assertTrue(container.isPresent());

    assertEquals(ContainerStatus.CREATED, container.get().getStatus());
  }

  @Test
  public void testVersion() {
    assumeTrue(DockerCM.Companion.isSupported());

    DockerCM d = new DockerCM();

    var version = d.getVersion();
    assertFalse(version.isEmpty());
    System.out.println(version);
    assertTrue(version.matches("Docker.+\\([0-9.]+\\)"));
  }

  @Test
  public void testStartStop() {
    assumeTrue(DockerCM.Companion.isSupported());

    DockerCM d = new DockerCM();

    // Pull an image we can actually start. (must be online)
    ApplicationContainer app = new ApplicationContainer();
    app.setImage("nginx");
    Optional<String> oContainerID = d.pullImage(app);

    // We expect a new container to be created
    assertTrue(oContainerID.isPresent());
    String containerID = oContainerID.get();
    wipes.add(containerID);
    assertNotNull(containerID);

    // we expect the container to be in list()
    List<ApplicationContainer> containers = d.list(false);
    Optional<ApplicationContainer> container =
        containers.stream().filter(c -> c.getId().equals(containerID)).findAny();
    assertTrue(container.isPresent());

    assertEquals(ContainerStatus.CREATED, container.get().getStatus());

    // Start container
    d.startContainer(containerID, null);

    // We now expect it in list of running containers
    containers = d.list(true);
    Optional<ApplicationContainer> runningContainer =
        containers.stream().filter(c -> c.getId().equals(containerID)).findAny();
    assertTrue(runningContainer.isPresent());
    assertEquals(ContainerStatus.RUNNING, runningContainer.get().getStatus());

    // Stop container
    d.stopContainer(containerID);

    // We expect it to be still in list of all containers
    containers = d.list(false);
    Optional<ApplicationContainer> stoppedContainer =
        containers.stream().filter(c -> c.getId().equals(containerID)).findAny();
    assertTrue(stoppedContainer.isPresent());
    assertEquals(ContainerStatus.EXITED, stoppedContainer.get().getStatus());
  }
}
