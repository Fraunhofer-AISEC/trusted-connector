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
package de.fhg.aisec.ids.cm.impl.trustx

import com.google.protobuf.InvalidProtocolBufferException
import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.cm.ContainerStatus
import de.fhg.aisec.ids.api.cm.Decision
import de.fhg.aisec.ids.api.cm.Direction
import de.fhg.aisec.ids.api.cm.Protocol
import de.fhg.aisec.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler
import de.fhg.aisec.ids.comm.unixsocket.TrustmeUnixSocketThread
import de.fraunhofer.aisec.trustme.Container.ContainerState
import de.fraunhofer.aisec.trustme.Control.ContainerStartParams
import de.fraunhofer.aisec.trustme.Control.ControllerToDaemon
import de.fraunhofer.aisec.trustme.Control.DaemonToController
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

/**
 * ContainerManager implementation for trust-x containers.
 *
 *
 * /dev/socket/cml-control Protobuf: control.proto container.proto für container configs
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
class TrustXCM @JvmOverloads constructor(socket: String = SOCKET) : ContainerManager {
    private var socketThread: TrustmeUnixSocketThread = TrustmeUnixSocketThread(socket)
    private var responseHandler: TrustmeUnixSocketResponseHandler = TrustmeUnixSocketResponseHandler()
    private val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.GERMANY)
        .withZone(ZoneId.systemDefault())

    private fun stateToStatusString(state: ContainerState): ContainerStatus {
        return when (state) {
            ContainerState.RUNNING, ContainerState.SETUP -> ContainerStatus.RUNNING
            else -> ContainerStatus.EXITED
        }
    }

    override fun list(onlyRunning: Boolean): List<ApplicationContainer> {
        LOG.debug("Starting list containers")
        val result: MutableList<ApplicationContainer> = ArrayList()
        val response = sendCommandAndWaitForResponse(ControllerToDaemon.Command.GET_CONTAINER_STATUS)
        try {
            val dtc = DaemonToController.parseFrom(response)
            val containerStats = dtc.containerStatusList
            for (cs in containerStats) {
                var container: ApplicationContainer
                if (!onlyRunning || ContainerState.RUNNING == cs.state) {
                    container = ApplicationContainer()
                    container.id = cs.uuid
                    container.image = ""
                    container.created = formatter.format(Instant.ofEpochSecond(cs.created))
                    // container.setStatus(cs.getState().name());
                    container.status = stateToStatusString(cs.state)
                    container.ports = emptyList()
                    container.names = cs.name
                    container.name = cs.name
                    container.size = ""
                    container.uptime = formatDuration(Duration.ofSeconds(cs.uptime))
                    container.signature = ""
                    container.owner = ""
                    container.description = "trustx container"
                    container.labels = emptyMap()
                    LOG.debug("List add Container: $container")
                    result.add(container)
                }
            }
        } catch (e: InvalidProtocolBufferException) {
            LOG.error("Response Length: " + response.size, e)
            LOG.error(
                """
    Response was: 
    ${bytesToHex(response)}
                """.trimIndent()
            )
        }
        return result
    }

    override fun wipe(containerID: String) {
        sendCommand(ControllerToDaemon.Command.CONTAINER_WIPE)
    }

    override fun startContainer(containerID: String, key: String?) {
        LOG.debug("Starting start container with ID {}", containerID)
        val ctdmsg = ControllerToDaemon.newBuilder()
        ctdmsg.command = ControllerToDaemon.Command.CONTAINER_START
        ctdmsg.addContainerUuids(containerID)
        val cspbld = ContainerStartParams.newBuilder()
        if (key != null) {
            cspbld.key = key
        }
        cspbld.noSwitch = true
        ctdmsg.containerStartParams = cspbld.build()
        try {
            val dtc = parseResponse(sendProtobufAndWaitForResponse(ctdmsg.build()))
            if (DaemonToController.Response.CONTAINER_START_OK != dtc.response) {
                LOG.error("Container start failed, response was {}", dtc.response)
            }
            LOG.error("Container start ok, response was {}", dtc.response)
        } catch (e: InvalidProtocolBufferException) {
            LOG.error("Protobuf error", e)
        }
    }

    override fun stopContainer(containerID: String) {
        LOG.debug("Starting stop container with ID {}", containerID)
        val ctdmsg = ControllerToDaemon.newBuilder()
        ctdmsg.command = ControllerToDaemon.Command.CONTAINER_STOP
        ctdmsg.addContainerUuids(containerID)
        sendProtobuf(ctdmsg.build())
    }

    override fun restartContainer(containerID: String) {
        sendCommand(ControllerToDaemon.Command.CONTAINER_STOP)
        sendCommand(ControllerToDaemon.Command.CONTAINER_START)
    }

    override fun pullImage(app: ApplicationContainer): String? {
        return null
    }

    override fun inspectContainer(containerID: String): String? {
        // TODO Auto-generated method stub
        return null
    }

    override fun getMetadata(containerID: String): String? {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
    }

    // TODO Auto-generated method stub
    override val version: String
        get() = "1.0"

    /**
     * Used for sending control commands to a device.
     *
     * @param command The command to be sent.
     */
    private fun sendCommand(command: ControllerToDaemon.Command) {
        val ctdmsg = ControllerToDaemon.newBuilder()
        ctdmsg.command = command
        sendProtobuf(ctdmsg.build())
    }

    /**
     * More flexible than the sendCommand method. Required when other parameters need to be set than
     * the Command
     *
     * @param ctd the control command
     */
    private fun sendProtobuf(ctd: ControllerToDaemon) {
        LOG.debug("sending message {}", ctd.command)
        LOG.debug(ctd.toString())
        val encodedMessage = ctd.toByteArray()
        try {
            socketThread.sendWithHeader(encodedMessage, responseHandler)
        } catch (e: IOException) {
            LOG.error(e.message, e)
        } catch (e: InterruptedException) {
            LOG.error(e.message, e)
        }
    }

    /**
     * Used for sending control commands to a device.
     *
     * @param command The command to be sent.
     * @return Success state.
     */
    private fun sendCommandAndWaitForResponse(command: ControllerToDaemon.Command): ByteArray {
        sendCommand(command)
        return responseHandler.waitForResponse()
    }

    /**
     * Used for sending control commands to a device.
     *
     * @param ctd The command to be sent.
     * @return Success state.
     */
    private fun sendProtobufAndWaitForResponse(ctd: ControllerToDaemon): ByteArray {
        sendProtobuf(ctd)
        return responseHandler.waitForResponse()
    }

    @Throws(InvalidProtocolBufferException::class)
    private fun parseResponse(response: ByteArray?): DaemonToController {
        return DaemonToController.parseFrom(response)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TrustXCM::class.java)
        private const val SOCKET = "/run/socket/cml-control"
        val isSupported: Boolean
            get() {
                val path = Paths.get(SOCKET)
                var exists = false
                if (Files.exists(path)) {
                    exists = true
                }
                return exists
            }
        private val hexArray = "0123456789ABCDEF".toCharArray()
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        private fun formatDuration(duration: Duration): String {
            val seconds = duration.seconds
            val absSeconds = abs(seconds)
            val days = dayString(absSeconds)
            val hoursAndMinutes = String.format("%d:%02d", absSeconds / 3600 / 24, absSeconds % 3600 / 60)
            return days + hoursAndMinutes
        }

        private fun dayString(seconds: Long): String {
            if (seconds != 0L) {
                val hours = seconds / 3600
                return when {
                    hours < 24 -> ""
                    hours < 48 -> "1 day "
                    else -> {
                        String.format("%d days ", hours / 24)
                    }
                }
            }
            return ""
        }
    }

    init {
        Thread(socketThread).apply {
            isDaemon = true
            start()
        }
    }
}
