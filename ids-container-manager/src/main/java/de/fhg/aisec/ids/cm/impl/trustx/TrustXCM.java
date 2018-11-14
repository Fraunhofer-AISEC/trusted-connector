/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.cm.impl.trustx;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.TrustmeUnixSocketThread;
import de.fraunhofer.aisec.trustme.Container.ContainerState;
import de.fraunhofer.aisec.trustme.Container.ContainerStatus;
import de.fraunhofer.aisec.trustme.Control.ContainerStartParams;
import de.fraunhofer.aisec.trustme.Control.ControllerToDaemon;
import de.fraunhofer.aisec.trustme.Control.ControllerToDaemon.Command;
import de.fraunhofer.aisec.trustme.Control.DaemonToController;
import de.fraunhofer.aisec.trustme.Control.DaemonToController.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContainerManager implementation for trust-x containers.
 *
 * <p>/dev/socket/cml-control Protobuf: control.proto container.proto für container configs
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public class TrustXCM implements ContainerManager {

  private static final Logger LOG = LoggerFactory.getLogger(TrustXCM.class);

  private static final String SOCKET = "/run/socket/cml-control";
  private TrustmeUnixSocketThread socketThread;
  private TrustmeUnixSocketResponseHandler responseHandler;
  private DateTimeFormatter formatter =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
          .withLocale(Locale.GERMANY)
          .withZone(ZoneId.systemDefault());

  public TrustXCM() {
    this(SOCKET);
  }

  public TrustXCM(String socket) {
    super();
    try {
      this.socketThread = new TrustmeUnixSocketThread(socket);
      this.responseHandler = new TrustmeUnixSocketResponseHandler();
      Thread t = new Thread(socketThread);
      t.setDaemon(true);
      t.start();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public List<ApplicationContainer> list(boolean onlyRunning) {
    LOG.debug("Starting list containers");
    List<ApplicationContainer> result = new ArrayList<>();
    byte[] response = sendCommandAndWaitForResponse(Command.GET_CONTAINER_STATUS);
    try {
      DaemonToController dtc = DaemonToController.parseFrom(response);
      List<ContainerStatus> containerStats = dtc.getContainerStatusList();
      for (ContainerStatus cs : containerStats) {
        ApplicationContainer container;
        if (!onlyRunning || ContainerState.RUNNING.equals(cs.getState())) {
          container = new ApplicationContainer();
          container.setId(cs.getUuid());
          container.setCreated(formatter.format(Instant.ofEpochSecond(cs.getCreated())));
          container.setStatus(cs.getState().name());
          container.setName(cs.getName());
          container.setUptime(formatDuration(Duration.ofSeconds(cs.getUptime())));
          result.add(container);
        }
      }
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Response Length: " + response.length, e);
      LOG.error("Response was: \n" + bytesToHex(response));
    }
    return result;
  }

  @Override
  public void wipe(String containerID) {
    sendCommand(Command.CONTAINER_WIPE);
  }

  @Override
  public void startContainer(String containerID, String key) {
    LOG.debug("Starting start container with ID {}", containerID);
    ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
    ctdmsg.setCommand(Command.CONTAINER_START);
    ctdmsg.addContainerUuids(containerID);
    ContainerStartParams.Builder cspbld = ContainerStartParams.newBuilder();
    if (key != null) {
      cspbld.setKey(key);
    }
    cspbld.setNoSwitch(true);

    ctdmsg.setContainerStartParams(cspbld.build());
    try {
      DaemonToController dtc = parseResponse(sendProtobufAndWaitForResponse(ctdmsg.build()));
      if (!Response.CONTAINER_START_OK.equals(dtc.getResponse())) {
        LOG.error("Container start failed, response was {}", dtc.getResponse());
      }
      LOG.error("Container start ok, response was {}", dtc.getResponse());
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Protobuf error", e);
    }
  }

  @Override
  public void stopContainer(String containerID) {
    LOG.debug("Starting stop container with ID {}", containerID);
    ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
    ctdmsg.setCommand(Command.CONTAINER_STOP);
    ctdmsg.addContainerUuids(containerID);
    sendProtobuf(ctdmsg.build());
  }

  @Override
  public void restartContainer(String containerID) {
    sendCommand(Command.CONTAINER_STOP);
    sendCommand(Command.CONTAINER_START);
  }

  @Override
  public Optional<String> pullImage(ApplicationContainer imageID) {
    // TODO Auto-generated method stub
    return Optional.<String>empty();
  }

  public static boolean isSupported() {
    Path path = Paths.get(SOCKET);
    boolean exists = false;
    if (Files.exists(path)) {
      exists = true;
    }
    return exists;
  }

  @Override
  public String inspectContainer(String containerID) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getMetadata(String containerID) {
    // TODO Auto-generated method stub
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
    // TODO Auto-generated method stub

  }

  @Override
  public String getVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Used for sending control commands to a device.
   *
   * @param command The command to be sent.
   */
  private void sendCommand(Command command) {
    ControllerToDaemon.Builder ctdmsg = ControllerToDaemon.newBuilder();
    ctdmsg.setCommand(command);
    sendProtobuf(ctdmsg.build());
  }

  /**
   * More flexible than the sendCommand method. Required when other parameters need to be set than
   * the Command
   *
   * @param ctd the control command
   */
  private void sendProtobuf(ControllerToDaemon ctd) {
    LOG.debug("sending message {}", ctd.getCommand());
    LOG.debug(ctd.toString());
    byte[] encodedMessage = ctd.toByteArray();
    try {
      socketThread.sendWithHeader(encodedMessage, responseHandler);
    } catch (IOException | InterruptedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Used for sending control commands to a device.
   *
   * @param command The command to be sent.
   * @return Success state.
   */
  private byte[] sendCommandAndWaitForResponse(Command command) {
    sendCommand(command);
    return responseHandler.waitForResponse();
  }

  /**
   * Used for sending control commands to a device.
   *
   * @param ctd The command to be sent.
   * @return Success state.
   */
  private byte[] sendProtobufAndWaitForResponse(ControllerToDaemon ctd) {
    sendProtobuf(ctd);
    return responseHandler.waitForResponse();
  }

  private DaemonToController parseResponse(byte[] response) throws InvalidProtocolBufferException {
    return DaemonToController.parseFrom(response);
  }

  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  private static String formatDuration(Duration duration) {
    long seconds = duration.getSeconds();
    long absSeconds = Math.abs(seconds);
    String days = dayString(absSeconds);
    String hoursAndMinutes =
        String.format("%d:%02d", (absSeconds / 3600) / 24, (absSeconds % 3600) / 60);
    return days + hoursAndMinutes;
  }

  private static String dayString(long seconds) {
    if (seconds != 0) {
      long hours = seconds / 3600;
      if (hours < 24) return "";
      else if (hours < 48) return "1 day ";
      else {
        return String.format("%d days ", hours / 24);
      }
    }
    return "";
  }
}
