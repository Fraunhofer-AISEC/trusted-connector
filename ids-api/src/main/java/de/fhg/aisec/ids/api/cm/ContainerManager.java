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
package de.fhg.aisec.ids.api.cm;

import java.util.List;
import java.util.Optional;

/**
 * Controls Application Containers of the underlying platform.
 *
 * <p>The container management layer can be Docker or trust-X.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public interface ContainerManager {

  /**
   * Returns the container management layer which is currently in use.
   *
   * <p>One of docker, trust-X, none.
   *
   * @return Version of the container management engine
   */
  String getVersion();

  /**
   * List currently installed containers.
   *
   * <p>The respective docker command is: <code>docker ps -a</code>.
   *
   * @param onlyRunning If set to true, only currently running containers are displayed.
   * @return List of containers
   */
  List<ApplicationContainer> list(boolean onlyRunning);

  /**
   * Wipes are container from disk, i.e. removes it irreversibly.
   *
   * @param containerID Hash of the container.
   * @throws NoContainerExistsException If the given container was not found.
   */
  void wipe(final String containerID) throws NoContainerExistsException;

  /**
   * Starts a container.
   *
   * <p>If the container is already running, this method will do nothing.
   *
   * <p>The container must already exist, otherwise an exception will be thrown.
   *
   * @param containerID ID of the container to start
   * @param key Key to start the container, if required by container management engine
   * @throws NoContainerExistsException If the given container was not found.
   */
  void startContainer(final String containerID, final String key)
      throws NoContainerExistsException;

  /**
   * Stops a container.
   *
   * <p>A stopped container does not execute any processes, but its persisted data is still present.
   * It can be started by <code>startContainer</code> again, i.e. the sequence stopContainer(x);
   * startContainer(X); has no effect. *
   *
   * <p>If the container is already stopped, this method will do nothing.
   *
   * <p>The container must already exist, otherwise an exception will be thrown.
   *
   * @param containerID ID of the container to stop
   * @throws NoContainerExistsException If the given container was not found.
   */
  void stopContainer(final String containerID) throws NoContainerExistsException;

  /**
   * Restarts a container without stopping it first.
   *
   * <p>The container must already exist, otherwise an exception will be thrown.
   *
   * @param containerID ID of the container to restart
   * @throws NoContainerExistsException If the given container was not found.
   */
  void restartContainer(final String containerID) throws NoContainerExistsException;

  /**
   * Retrieves configuration data about a container.
   *
   * <p>The data format returned will depend on the underlying CML implementation. As for docker,
   * this command will return the output of the command <code>docker inspect</code>.
   *
   * <p>If meta data is stored in container labels, the result of <code>getMetaData</code> will also
   * be contained in the result of this method.
   *
   * @param containerID ID of the container. If does not exist, a NoContainerExistsException will be
   *     thrown.
   * @return Information about the queried container, in implementation-dependent format
   * @throws NoContainerExistsException If the given container was not found.
   */
  String inspectContainer(final String containerID) throws NoContainerExistsException;

  /**
   * Returns metadata associated with the service running in the container.
   *
   * <p>The data format returned depends on the meta data implementation, but will usually be RDF.
   *
   * @param containerID ID of the container to get metadata for
   * @return An object with metadata about the container, in implementation-dependent format
   * @throws NoContainerExistsException If the given container was not found.
   */
  Object getMetadata(final String containerID) throws NoContainerExistsException;

  /**
   * Pulls an image from the online registry.
   *
   * <p>The online registry can be given as a URL. If it is not given, the standard Docker registry
   * is used in case of a Docker CML implementation.
   *
   * <p>This method blocks until the image has been pulled or an exception has occurred. As this is
   * a long running operation, it should always be called in a separate thread.
   *
   * @param app Metadata about the container to be created and started with given image data
   * @return ID of the container, if started successfully
   * @throws NoContainerExistsException If the given container was not found.
   */
  Optional<String> pullImage(final ApplicationContainer app)
      throws NoContainerExistsException;

  /**
   * Configures an IP rule for a container.
   *
   * <p>By default, containers do not have IP connectivity, i.e. all inbound and outbound traffic is
   * blocked.
   *
   * <p>This method can be used to allow specific communication channels from/to a container.
   *
   * @param containerID ID of the container. It must exist, otherwise a NoContainerExistsException
   *     will be thrown.
   * @param direction INBOUND for traffic going into the container or OUTBOUND for traffic leaving
   *     the container. Replies within established TCP sessions will always be allowed, there is no
   *     need to configure them.
   * @param srcPort IP source port.
   * @param dstPort IP destination port.
   * @param srcDstRange Remote IP, i.e. for OUTBOUND traffic, IP range of the destination. For
   *     INBOUND traffic, IP range of the source.
   * @param protocol TPC or UDP.
   * @param decision ALLOW, DENY, or DROP
   */
  void setIpRule(
      String containerID,
      Direction direction,
      int srcPort,
      int dstPort,
      String srcDstRange,
      Protocol protocol,
      Decision decision);
}
