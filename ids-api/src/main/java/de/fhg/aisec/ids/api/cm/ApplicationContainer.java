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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean representing an "Application Container" (aka a docker container).
 *
 * @author julian.schuette@aisec.fraunhofer.de
 *     <p>Complies with Portainer templates:
 *     https://github.com/portainer/portainer/blob/develop/app/docker/models/template.js
 */
@JsonIgnoreProperties
public class ApplicationContainer {
  // Trusted Connector-specific properties:
  private String id;
  private String created;
  private String status;
  private List<String> ports;
  private String names;
  private String size;
  private String uptime;
  private String signature;
  private String owner;
  private String image;

  // Portainer attributes:
  private Object repository;
  private String type;
  private String name;
  private String hostname;
  private String title;
  private String description;
  private String note;
  private List<String> categories = new ArrayList<>();
  private String platform = "linux";
  private String logo;
  private String registry = "";
  private String command = "";
  private String network;
  private List<Map<String, Object>> env = new ArrayList<>();
  private boolean privileged = false;
  private boolean interactive = false;
  private String restartPolicy = "always";
  private Map<String, Object> labels = new HashMap<>();
  private List<Object> volumes = new ArrayList<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getPorts() {
    return ports;
  }

  public void setPorts(List<String> ports) {
    this.ports = ports;
  }

  public String getNames() {
    return names;
  }

  public void setNames(String names) {
    this.names = names;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getUptime() {
    return uptime;
  }

  public void setUptime(String uptime) {
    this.uptime = uptime;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getRegistry() {
    return registry;
  }

  public void setRegistry(String registry) {
    this.registry = registry;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public List<Map<String, Object>> getEnv() {
    return env;
  }

  public void setEnv(List<Map<String, Object>> env) {
    this.env = env;
  }

  public boolean isPrivileged() {
    return privileged;
  }

  public void setPrivileged(boolean privileged) {
    this.privileged = privileged;
  }

  public boolean isInteractive() {
    return interactive;
  }

  public void setInteractive(boolean interactive) {
    this.interactive = interactive;
  }

  public String getRestartPolicy() {
    return restartPolicy;
  }

  public void setRestartPolicy(String restartPolicy) {
    this.restartPolicy = restartPolicy;
  }

  public Map<String, Object> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, Object> labels) {
    this.labels = labels;
  }

  public List<Object> getVolumes() {
    return volumes;
  }

  public void setVolumes(List<Object> volumes) {
    this.volumes = volumes;
  }

  @Override
  public String toString() {
    return "ApplicationContainer [id="
        + id
        + ", image="
        + image
        + ", created="
        + created
        + ", status="
        + status
        + ", ports="
        + ports
        + ", names="
        + names
        + ", size="
        + size
        + ", uptime="
        + uptime
        + ", signature="
        + signature
        + ", owner="
        + owner
        + ", description="
        + description
        + "]";
  }

  public Object getRepository() {
    return repository;
  }

  public void setRepository(Object repository) {
    this.repository = repository;
  }
}
