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
package de.fhg.aisec.ids.api.policy;

import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Data structure holding a decision request which is sent to the PDP. The PDP is expected to answer
 * with a PolicyDecision object.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
public class DecisionRequest {
  /** The processor that data is received from */
  @NonNull private ServiceNode from;

  /** The Processor that the data is to be sent to */
  @NonNull private ServiceNode to;

  /** Properties of the message (e.g., labels) */
  @NonNull private Map<String, Object> msgCtx;

  /** Properties of the environment */
  @Nullable private Map<String, Object> envCtx;

  public DecisionRequest(
      @NonNull ServiceNode from,
      @NonNull ServiceNode to,
      @NonNull Map<String, Object> msgCtx,
      @Nullable Map<String, Object> envCtx) {
    super();
    this.from = from;
    this.to = to;
    this.msgCtx = msgCtx;
    this.envCtx = envCtx;
  }

  /**
   * Returns the source, i.e. the origin of the communication for which a decision is requested.
   *
   * @return The processor that data is received from
   */
  @NonNull
  public ServiceNode getFrom() {
    return from;
  }

  /**
   * Returns the sink, i.e. the endpoint of the communication for which a decision is requested.
   *
   * @return The Processor that the data is to be sent to
   */
  @NonNull
  public ServiceNode getTo() {
    return to;
  }

  /**
   * A decision context may hold additional information about the message/event. It is passed as
   * attributes to the PDP.
   *
   * <p>The context may include - timestamps - route ids - etc.
   *
   * @return Properties of the Exchange
   */
  @NonNull
  public Map<String, Object> getProperties() {
    return msgCtx;
  }

  /**
   * A decision context may hold additional information about the overall system environment of the
   * PEP.. It is passed as attributes to the PDP.
   *
   * <p>The context may include - a reference to previously taken decisions for the sake of caching
   * - a reason for the request - identifiers of available components - etc.
   *
   * @return Additional environment properties
   */
  @Nullable
  public Map<String, Object> getEnvironmentCtx() {
    return envCtx;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DecisionRequest that = (DecisionRequest) o;
    return Objects.equals(from, that.from)
        && Objects.equals(to, that.to)
        && Objects.equals(msgCtx, that.msgCtx)
        && Objects.equals(envCtx, that.envCtx);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, msgCtx, envCtx);
  }
}
