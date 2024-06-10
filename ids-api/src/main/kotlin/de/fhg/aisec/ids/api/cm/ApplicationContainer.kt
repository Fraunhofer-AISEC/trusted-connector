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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.net.InetAddress

/**
 * Bean representing an "Application Container" (aka a docker container).
 *
 * @author julian.schuette@aisec.fraunhofer.de
 *
 * Complies with Portainer templates:
 * https://github.com/portainer/portainer/blob/develop/app/docker/models/template.js
 */
@JsonIgnoreProperties
class ApplicationContainer {
    // Trusted Connector-specific properties:
    var id: String? = null
    var created: String? = null
    var status: ContainerStatus? = null
    var ports: List<String> = emptyList()
    var names: String? = null
    var size: String? = null
    var uptime: String? = null
    var signature: String? = null
    var owner: String? = null
    var image: String? = null
    var imageId: String? = null
    var repoDigest: List<String>? = null
    var cmd: List<String>? = null
    var entrypoint: List<String>? = null
    var imageCmd: List<String>? = null
    var imageEntrypoint: List<String>? = null
    var ipAddresses: List<InetAddress> = emptyList()

    // Portainer attributes:
    var repository: Any? = null
    var type: String? = null
    var name: String? = null
    var hostname: String? = null
    var title: String? = null
    var description: String? = null
    var note: String? = null
    var categories: List<String> = emptyList()
    var platform = "linux"
    var logo: String? = null
    var registry = ""
    var command = ""
    var network: String? = null
    var env: List<Map<String, Any>> = emptyList()
    var privileged = false
    var restartPolicy = "always"
    var labels: Map<String, Any> = emptyMap()
    var volumes: List<Any> = emptyList()

    override fun toString(): String =
        (
            "ApplicationContainer [id=" +
                id +
                ", image=" +
                image +
                ", created=" +
                created +
                ", status=" +
                status +
                ", ports=" +
                ports +
                ", names=" +
                names +
                ", size=" +
                size +
                ", uptime=" +
                uptime +
                ", signature=" +
                signature +
                ", owner=" +
                owner +
                ", description=" +
                description +
                "]"
        )
}
