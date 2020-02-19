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

import com.amihaiemil.docker.Container
import com.amihaiemil.docker.Docker
import com.amihaiemil.docker.Image
import com.amihaiemil.docker.LocalDocker
import de.fhg.aisec.ids.api.cm.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.json.Json
import javax.json.JsonValue
import kotlin.math.abs


/**
 * ContainerManager implementation for Docker containers.
 *
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
class DockerCM : ContainerManager {
    companion object {
        private val LOG = LoggerFactory.getLogger(DockerCM::class.java)
        private lateinit var DOCKER_CLIENT: Docker
        private val PERIOD_UNITS = listOf<TemporalUnit>(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS)
        private val DURATION_UNITS = listOf<TemporalUnit>(ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS)
        /**
         * Returns true if Docker is supported.
         *
         * @return Whether docker socket is available on the system
         */
        val isSupported: Boolean
            get() {
                return try {
                    DOCKER_CLIENT.ping()
                } catch (e: Exception) {
                    when(e) {
                        is UninitializedPropertyAccessException -> LOG.warn("Docker client is not available")
                        else -> LOG.error(e.message, e)
                    }
                    false
                }
            }

        init {
            try { // We have to modify the thread class loader for docker-java-api to find its config
                val threadContextClassLoader = Thread.currentThread().contextClassLoader
                Thread.currentThread().contextClassLoader = LocalDocker::class.java.classLoader
                DOCKER_CLIENT = LocalDocker(File("/var/run/docker.sock"))
                Thread.currentThread().contextClassLoader = threadContextClassLoader
            } catch (x: Exception) {
                LOG.error("Error initializing docker client", x)
            }
        }

        /**
         * Human readable memory sizes
         * Credits: aioobe, https://stackoverflow.com/questions
         * /3758606/how-to-convert-byte-size-into-human-readable-format-in-java
         */
        private fun humanReadableByteCount(bytes: Long): String? {
            val b = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
            return when {
                b < 1024L -> {
                    "$bytes B"
                }
                b <= 0xfffccccccccccccL shr 40 -> {
                    String.format("%.2f KiB", (bytes / 0x400).toFloat())
                }
                b <= 0xfffccccccccccccL shr 30 -> {
                    String.format("%.2f MiB", (bytes / 0x100000).toFloat())
                }
                b <= 0xfffccccccccccccL shr 20 -> {
                    String.format("%.2f GiB", (bytes / 0x40000000).toFloat())
                }
                b <= 0xfffccccccccccccL shr 10 -> {
                    String.format("%.2f TiB", (bytes / 0x10000000000).toFloat())
                }
                b <= 0xfffccccccccccccL -> {
                    String.format("%.2f PiB", ((bytes shr 10) / 0x10000000000).toFloat())
                }
                else -> {
                    String.format("%.2f EiB", ((bytes shr 20) / 0x10000000000).toFloat())
                }
            }
        }

        private fun formatDuration(a: ZonedDateTime, b: ZonedDateTime): String {
            val period = Period.between(a.toLocalDate(), b.toLocalDate())
            var duration = ChronoUnit.SECONDS.between(a.toLocalTime(), b.toLocalTime())
            val units: MutableList<String> = LinkedList()
            for (unit in PERIOD_UNITS) {
                val elapsed = period[unit]
                if (elapsed > 0) {
                    units.add(elapsed.toString() + " " + unit.toString().toLowerCase())
                }
            }
            for (unit in DURATION_UNITS) {
                val unitDuration = unit.duration.toSeconds()
                val elapsed = duration / unitDuration
                if (elapsed > 0) {
                    units.add(elapsed.toString() + " " + unit.toString().toLowerCase())
                }
                duration %= unitDuration
            }
            return java.lang.String.join(", ", units)
        }
    }

    private fun getContainerStream(
            all: Boolean = false,
            filters: Map<String, Iterable<String>>? = null,
            withSize: Boolean = false): Stream<Container> {
        val filteredContainers = DOCKER_CLIENT
                .containers()
                .filter(filters ?: emptyMap())
                .withSize(withSize)
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        if (all) filteredContainers.all() else filteredContainers.iterator(),
                        Spliterator.ORDERED
                ), false)
    }

    private fun getImageStream(filters: Map<String, Iterable<String>>?): Stream<Image> {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        DOCKER_CLIENT
                                .images()
                                .filter(filters ?: emptyMap()).iterator(),
                        Spliterator.ORDERED
                ), false)
    }

    override fun list(onlyRunning: Boolean): List<ApplicationContainer> {
        return getContainerStream(true, withSize = true).map { c: Container ->
            try {
                val info = c.inspect()
                val state = info.getJsonObject("State")
                val config = info.getJsonObject("Config")
                val running = state.getBoolean("Running")
                val startedAt = state.getString("StartedAt")
                val name = info.getString("Name").substring(1)
                val ports = info.getJsonObject("NetworkSettings").getJsonObject("Ports")
                val labels = config.getJsonObject("Labels")
                val app = ApplicationContainer()
                app.id = c.containerId()
                app.image = config.getString("Image")
                app.size = "${humanReadableByteCount((c["SizeRw"] ?: 0).toString().toLong())} RW (data), " +
                        "${humanReadableByteCount((c["SizeRootFs"] ?: 0).toString().toLong())} RO (layers)"
                app.created = info.getString("Created")
                app.status = ContainerStatus.valueOf(state.getString("Status").toUpperCase())
                app.ports = ports.entries
                        .map { e: Map.Entry<String, JsonValue> -> e.key + ":" + e.value.toString() }
                        .toList()
                app.names = name
                if (running) {
                    app.uptime = formatDuration(
                            ZonedDateTime.parse(startedAt), OffsetDateTime.now(ZoneId.of("Z")).toZonedDateTime())
                } else {
                    app.uptime = "-"
                }
                app.signature = labels.getOrDefault("ids.signature", JsonValue.NULL).toString()
                app.owner = labels.getOrDefault("ids.owner", JsonValue.NULL).toString()
                app.description = labels.getOrDefault("ids.description", JsonValue.NULL).toString()
                app.labels = labels.entries.stream()
                        .collect(Collectors.toMap({ it.key }, { it.value }))
                return@map app
            } catch (e: IOException) {
                LOG.error("Container iteration error occurred, skipping container with id " + c.containerId(), e)
                return@map null
            }
        }.filter { it != null }.map { it as ApplicationContainer }.collect(Collectors.toList())
    }

    private fun getContainer(containerID: String): Container {
        val filters = HashMap<String, Iterable<String>>()
        filters["id"] = listOf(containerID)
        val optionalContainer = getContainerStream(true, filters).findFirst()
        if (optionalContainer.isEmpty) {
            throw NoContainerExistsException("The container with ID $containerID has not been found!")
        } else {
            return optionalContainer.get()
        }
    }

    override fun wipe(containerID: String) {
        val container = getContainer(containerID)
        try {
            LOG.info("Wiping containerID $containerID")
            container.remove(false, true, false)
            val filters: MutableMap<String, Iterable<String>> = HashMap()
            filters["reference"] = listOf(container.getString("Image"))
            val optionalImage = getImageStream(filters).findFirst()
            if (optionalImage.isEmpty) {
                LOG.warn("The image to be deleted (filters: {}) was not found!", filters)
                return
            }
            LOG.info("Wiping image related to containerID $containerID")
            optionalImage.get().delete()
        } catch (e: Exception) {
            LOG.error(e.message, e)
        }
    }

    override fun startContainer(containerID: String, key: String?) {
        val container = getContainer(containerID)
        try {
            container.start()
        } catch (e: IOException) {
            LOG.error("Error while starting container $containerID", e)
        }
    }

    override fun stopContainer(containerID: String) {
        val container = getContainer(containerID)
        try {
            container.stop()
        } catch (e: IOException) {
            LOG.error("Error while stopping container $containerID", e)
        }
    }

    override fun restartContainer(containerID: String) {
        val container = getContainer(containerID)
        try {
            container.restart()
        } catch (e: IOException) {
            LOG.error("Error while restarting container $containerID", e)
        }
    }

    override fun pullImage(app: ApplicationContainer): Optional<String> {
        try {
            val imageInfo = app.image.split(":").toTypedArray()
            val tag = if (imageInfo.size == 2) imageInfo[1] else "latest"
            LOG.info("Pulling container image {} with tag {}", imageInfo[0], tag)
            // Pull image from std docker registry
            DOCKER_CLIENT.images().pull(imageInfo[0], tag)

            // Instantly create a container from that image, but do not start it yet.
            LOG.info("Creating container instance from image {}", app.image)
            // Create the name
            val containerName: String = if (app.name != null) {
                app.name
            } else {
                defaultContainerName(app.image)
            }
            val container = Json.createObjectBuilder()
            val hostConfig = Json.createObjectBuilder()
            // Set image
            container.add("Image", app.image)
            // Add published ports and port bindings
            if (app.ports != null) {
                val exposedPorts = Json.createObjectBuilder()
                val portBindings = Json.createObjectBuilder()
                val portRegex = ("(?:((?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}" +
                        "(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])):)?" +
                        "([0-9]+):([0-9]+)(?:/(tcp|udp))?").toRegex()
                for (port in app.ports) {
                    val match = portRegex.matchEntire(port)
                    if (match == null) {
                        LOG.warn("Port definition {} does not match the pattern " +
                                "[IPv4:]HostPort:ContainerPort[/tcp|udp], ignoring it", port)
                    } else {
                        val groups = match.groupValues
                        val protocol = (if (groups[4].isEmpty()) "tcp" else groups[4])
                        exposedPorts.add(groups[3] + "/" + protocol, JsonValue.EMPTY_JSON_OBJECT)
                        val portBinding = Json.createObjectBuilder()
                        if (groups[1].isNotEmpty()) {
                            portBinding.add("HostIp", groups[1])
                        }
                        portBinding.add("HostPort", groups[2])
                        portBindings.add(groups[3] + "/" + protocol, portBinding)
                    }
                }
                container.add("ExposedPorts", exposedPorts)
                hostConfig.add("PortBindings", portBindings)
            }
            if (app.env != null) {
                val envJson = Json.createArrayBuilder()
                for (env in app.env) {
                    val varName = env["name"] as String?
                    val varValue = env["set"] as String?
                    envJson.add("$varName=$varValue")
                }
                container.add("Env", envJson)
            }
            // Sets label(s)
            val labels = Json.createObjectBuilder()
            labels.add("created", Instant.now().toEpochMilli().toString())
            if (app.labels != null) {
                app.labels.forEach { labels.add(it.key, it.value.toString()) }
            }
            container.add("Labels", labels)
            // Set restart policy
            if (app.restartPolicy != null) {
                hostConfig.add(
                        "RestartPolicy", Json.createObjectBuilder().add("Name", app.restartPolicy))
            }
            // Set privileged state
            if (app.isPrivileged) {
                hostConfig.add("Privileged", JsonValue.TRUE)
            }
            container.add("HostConfig", hostConfig)
            val c = DOCKER_CLIENT.containers().create(containerName, container.build())

            return Optional.of(c.containerId())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Throwable) {
            LOG.error(e.message, e)
        }
        return Optional.empty()
    }

    /**
     * Returns the default containerName that will be given to a container of image "imageID".
     *
     *
     * For example, an imageID "shiva1029/weather" will be turned into "weather-shiva1029".
     *
     * @param imageName The name of the image
     * @return The name of the container, based on the image name
     */
    private fun defaultContainerName(imageName: String): String {
        if (imageName.indexOf('/') > -1 && imageName.indexOf('/') < imageName.length - 1) {
            val name = imageName.substring(imageName.indexOf('/') + 1)
            var rest = imageName.replace(name, "").replace('/', '-')
            rest = rest.substring(0, rest.length - 1)
            return "$name-$rest".replace(":", "_")
        }
        return imageName.replace(":", "_")
    }

    /**
     * Provides the labels associated with a given container
     *
     * @param containerID The ID of the container to query labels from
     */
    override fun getMetadata(containerID: String): Map<String, Any> {
        val labels = HashMap<String, Any>()
        getContainer(containerID).inspect().getJsonObject("Config").getJsonObject("Labels")
        return labels
    }

    override fun setIpRule(
            containerID: String,
            direction: Direction,
            srcPort: Int,
            dstPort: Int,
            srcDstRange: String,
            protocol: Protocol,
            decision: Decision) { // TODO Not implemented yet
    }

    /**
     * Returns the result of the docker inspect API call as JSON
     *
     * @param containerID container id
     * @return container information
     */
    override fun inspectContainer(containerID: String): String {
        return getContainer(containerID).inspect().toString()
    }

    /**
     * Returns the version of docker on the system
     */
    override fun getVersion(): String {
        val version = DOCKER_CLIENT.version()
        return "${version.platformName()} (${version.version()})"
    }
}