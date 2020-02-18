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
package de.fhg.aisec.ids.cm;

import de.fhg.aisec.ids.api.cm.*;
import de.fhg.aisec.ids.cm.impl.docker.DockerCM;
import de.fhg.aisec.ids.cm.impl.dummy.DummyCM;
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main entry point of the Container Management Layer.
 *
 * <p>This class is mainly a facade for the actual CML implementation, which can either be Docker or
 * trust-X.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
@Component(name = "ids-cml", immediate = true)
public class ContainerManagerService implements ContainerManager {
  private static final Logger LOG = LoggerFactory.getLogger(ContainerManagerService.class);
  private ContainerManager containerManager = null;

  @Activate
  protected void activate() {
    LOG.info("Activating Container Manager");
    // When activated, try to set container management instance
    containerManager = getDefaultCM();
    LOG.info("Default container management is {}", containerManager);
  }

  @Deactivate
  protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
    containerManager = null;
  }

  private ContainerManager getDefaultCM() {
    ContainerManager result;
    if (TrustXCM.isSupported()) {
      result = new TrustXCM();
    } else if (DockerCM.Companion.isSupported()) {
      result = new DockerCM();
    } else {
      LOG.warn("No supported container management layer found. Using dummy");
      result = new DummyCM();
    }
    return result;
  }

  @Override
  public List<ApplicationContainer> list(boolean onlyRunning) {
    return containerManager.list(onlyRunning);
  }

  @Override
  public void wipe(String containerID) {
    try {
      containerManager.wipe(containerID);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void startContainer(String containerID, String key) {
    try {
      containerManager.startContainer(containerID, key);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void stopContainer(String containerID) {
    try {
      containerManager.stopContainer(containerID);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public void restartContainer(String containerID) {
    try {
      containerManager.restartContainer(containerID);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public Optional<String> pullImage(ApplicationContainer app) {
    try {
      return containerManager.pullImage(app);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
    return Optional.empty();
  }

  @Override
  public String inspectContainer(String containerID) {
    try {
      return containerManager.inspectContainer(containerID);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
    return "";
  }

  @Override
  public Object getMetadata(String containerID) {
    try {
      return containerManager.getMetadata(containerID);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public void setIpRule(
      String containerID,
      Direction direction,
      int srcPort,
      int dstPort,
      String srcDstRange,
      Protocol protocol,
      Decision decision) {
    containerManager.setIpRule(
        containerID, direction, srcPort, dstPort, srcDstRange, protocol, decision);
  }

  @Override
  public String getVersion() {
    return containerManager.getVersion();
  }
}
