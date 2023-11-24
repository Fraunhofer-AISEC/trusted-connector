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

import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.dataflowcontrol.CamelInterceptor
import java.net.InetAddress
import java.net.URI

class DockerImageConstraint(dockerUri: URI) : LuconConstraint {
    private val hash: String
    private val port: Int

    init {
        // Extracting hash and port of containerUri given by CamelRoute
        val hashPart = dockerUri.path.split("/").last()
        if (!hashPart.startsWith("sha256-")) {
            throw LuconException(
                "Invalid docker URI for UC, last path component must start with \"sha256-\"!"
            )
        }
        hash = hashPart.substring(7)
        port =
            try {
                dockerUri.fragment.toInt().also { assert(it in 1..65535) }
            } catch (nfe: NumberFormatException) {
                throw LuconException(
                    "Invalid docker URI for UC, fragment must represent a valid port number!",
                    nfe
                )
            } catch (ae: AssertionError) {
                throw LuconException(
                    "Invalid docker URI for UC, ${dockerUri.fragment} is not a valid port number!"
                )
            }
    }

    private fun checkEntrypointAndCmd(targetContainers: List<ApplicationContainer>) {
        targetContainers.forEach {
            // If neither entrypoint nor cmd are present, it indicates an invalid condition (e.g. no Docker CM)
            if (it.entrypoint == null && it.cmd == null) {
                throw LuconException(
                    "Container $it has neither an entrypoint, nor a command, " +
                        "which is considered an invalid condition for enforcement!"
                )
            }
            // Check whether the container entrypoint matches the image entrypoint
            if (it.entrypoint != it.imageEntrypoint) {
                throw LuconException(
                    "Entrypoint override found, \"${it.entrypoint}\" of container $it " +
                        "overrides \"${it.imageEntrypoint}\" of its image, which is prohibited!"
                )
            }
            // Check whether the container cmd matches the image cmd
            if (it.cmd != it.imageCmd) {
                throw LuconException(
                    "Command override found, \"${it.cmd}\" of container $it " +
                        "overrides \"${it.imageCmd}\" of its image, which is prohibited!"
                )
            }
        }
    }

    override fun checkEnforcible(
        context: EnforcementContext,
        permission: LuconPermission
    ) {
        // Check local Docker containers' image hashes and port against Camel route's endpoint
        CamelInterceptor.containerManager?.let { cm ->
            val endpointIPs = InetAddress.getAllByName(context.endpointUri.host).toHashSet()

            // Gather meta of all currently running Docker containers and cache it for other DockerImageConstraint
            // instances using the same EnforcementContext
            @Suppress("UNCHECKED_CAST")
            val runningContainers =
                context.enforcementCache.computeIfAbsent("runningDockerContainers") {
                    cm.list(true)
                } as List<ApplicationContainer>
            val targetContainers =
                runningContainers.filter { container ->
                    // From running docker containers, get all with the given hash.
                    // Normally there is only one hash type, but there can be more.
                    // Currently, requested type is only sha256 (e.g. sha3 or others may be added in the future).
                    (
                        container.imageId?.split(":")?.last() == hash ||
                            container.repoDigest?.any { it.split(":").last() == hash } ?: false
                    ) &&
                        // Additionally, filter relevant containers by their IP address(es)
                        container.ipAddresses.any(endpointIPs::contains)
                }
            checkEntrypointAndCmd(targetContainers)
            // Collect all ip addresses of targeted containers in one HashSet.
            val allowedIPs = targetContainers.flatMap { it.ipAddresses }.toHashSet()
            // Check whether ALL endpoint's ip-addresses belong to an allowed container.
            if (endpointIPs.all(allowedIPs::contains)) {
                val targetPort = context.endpointUri.port
                if (targetPort != port) {
                    throw LuconException(
                        "Image is OK, but transfer to port $targetPort is not permitted, expected $port"
                    )
                }
            } else {
                throw LuconException(
                    "One or more of the IP addresses of the host ${context.endpointUri.host} " +
                        "(${endpointIPs.map(InetAddress::getHostAddress)}) " +
                        "do not belong to the IPs of the permitted containers " +
                        "(${allowedIPs.map(InetAddress::getHostAddress)})"
                )
            }
        }
            ?: throw LuconException(
                "ContainerManager is not available, cannot verify container-binding contract!"
            )
    }

    override fun enforce(context: EnforcementContext) {
        // Nothing to do here, this constraint does only consist of the enforceability check.
    }
}
