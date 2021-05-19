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
package de.fhg.aisec.ids.api.cm

/**
 * Controls Application Containers of the underlying platform.
 *
 *
 * The container management layer can be Docker or trust-X.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
interface ContainerManager {
    /**
     * Returns the container management layer which is currently in use.
     *
     *
     * One of docker, trust-X, none.
     *
     * @return Version of the container management engine
     */
    val version: String

    /**
     * List currently installed containers.
     *
     *
     * The respective docker command is: `docker ps -a`.
     *
     * @param onlyRunning If set to true, only currently running containers are displayed.
     * @return List of containers
     */
    fun list(onlyRunning: Boolean): List<ApplicationContainer>

    /**
     * Wipes are container from disk, i.e. removes it irreversibly.
     *
     * @param containerID Hash of the container.
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun wipe(containerID: String)

    /**
     * Starts a container.
     *
     *
     * If the container is already running, this method will do nothing.
     *
     *
     * The container must already exist, otherwise an exception will be thrown.
     *
     * @param containerID ID of the container to start
     * @param key Key to start the container, if required by container management engine
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun startContainer(containerID: String, key: String?)

    /**
     * Stops a container.
     *
     *
     * A stopped container does not execute any processes, but its persisted data is still present.
     * It can be started by `startContainer` again, i.e. the sequence stopContainer(x);
     * startContainer(X); has no effect. *
     *
     *
     * If the container is already stopped, this method will do nothing.
     *
     *
     * The container must already exist, otherwise an exception will be thrown.
     *
     * @param containerID ID of the container to stop
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun stopContainer(containerID: String)

    /**
     * Restarts a container without stopping it first.
     *
     *
     * The container must already exist, otherwise an exception will be thrown.
     *
     * @param containerID ID of the container to restart
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun restartContainer(containerID: String)

    /**
     * Retrieves configuration data about a container.
     *
     *
     * The data format returned will depend on the underlying CML implementation. As for docker,
     * this command will return the output of the command `docker inspect`.
     *
     *
     * If meta data is stored in container labels, the result of `getMetaData` will also
     * be contained in the result of this method.
     *
     * @param containerID ID of the container. If does not exist, a NoContainerExistsException will be
     * thrown.
     * @return Information about the queried container, in implementation-dependent format
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun inspectContainer(containerID: String): String?

    /**
     * Returns metadata associated with the service running in the container.
     *
     *
     * The data format returned depends on the meta data implementation, but will usually be RDF.
     *
     * @param containerID ID of the container to get metadata for
     * @return An object with metadata about the container, in implementation-dependent format
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun getMetadata(containerID: String): Any?

    /**
     * Pulls an image from the online registry.
     *
     *
     * The online registry can be given as a URL. If it is not given, the standard Docker registry
     * is used in case of a Docker CML implementation.
     *
     *
     * This method blocks until the image has been pulled or an exception has occurred. As this is
     * a long running operation, it should always be called in a separate thread.
     *
     * @param app Metadata about the container to be created and started with given image data
     * @return ID of the container, if started successfully
     * @throws NoContainerExistsException If the given container was not found.
     */
    @Throws(NoContainerExistsException::class)
    fun pullImage(app: ApplicationContainer): String?

    /**
     * Configures an IP rule for a container.
     *
     *
     * By default, containers do not have IP connectivity, i.e. all inbound and outbound traffic is
     * blocked.
     *
     *
     * This method can be used to allow specific communication channels from/to a container.
     *
     * @param containerID ID of the container. It must exist, otherwise a NoContainerExistsException
     * will be thrown.
     * @param direction INBOUND for traffic going into the container or OUTBOUND for traffic leaving
     * the container. Replies within established TCP sessions will always be allowed, there is no
     * need to configure them.
     * @param srcPort IP source port.
     * @param dstPort IP destination port.
     * @param srcDstRange Remote IP, i.e. for OUTBOUND traffic, IP range of the destination. For
     * INBOUND traffic, IP range of the source.
     * @param protocol TPC or UDP.
     * @param decision ALLOW, DENY, or DROP
     */
    fun setIpRule(
        containerID: String,
        direction: Direction,
        srcPort: Int,
        dstPort: Int,
        srcDstRange: String,
        protocol: Protocol,
        decision: Decision
    )
}
