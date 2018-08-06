/*-
 * ========================LICENSE_START=================================
 * ids-api
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
package de.fhg.aisec.ids.api.policy;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class ServiceNode {
  private String endpoint;
  private Set<String> properties;
  private Set<String> capabilities;

  public ServiceNode(String endpoint, Set<String> properties, Set<String> capabilities) {
    super();
    this.endpoint = endpoint;
    this.properties = properties != null ? properties : Collections.emptySet();
    this.capabilities = capabilities != null ? capabilities : Collections.emptySet();
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Set<String> getProperties() {
    return properties;
  }

  public void setProperties(Set<String> properties) {
    this.properties = properties;
  }

  public Set<String> getCapabilties() {
    return capabilities;
  }

  public void setCapabilities(Set<String> capabilities) {
    this.capabilities = capabilities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceNode that = (ServiceNode) o;
    return Objects.equals(endpoint, that.endpoint)
        && Objects.equals(properties, that.properties)
        && Objects.equals(capabilities, that.capabilities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoint, properties, capabilities);
  }
}
