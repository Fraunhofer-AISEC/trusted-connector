/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
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
package de.fhg.aisec.ids.rm;

import de.fhg.aisec.ids.api.policy.*;
import de.fhg.aisec.ids.api.router.RouteManager;
import org.apache.camel.*;
import org.apache.camel.model.RouteDefinition;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PolicyEnforcementPoint implements AsyncProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcementPoint.class);
  private final NamedNode node;
  private final Processor target;
  private final RouteManager rm;

  PolicyEnforcementPoint(
          @NonNull NamedNode node,
          @NonNull Processor target,
          @NonNull RouteManager rm) {
    this.node = node;
    this.target = target;
    this.rm = rm;
  }

  /**
   * The method performs flow control and calls Exchange.setException() when necessary
   *
   * @param exchange The exchange object to check
   * @return Whether target.process() is to be called for this exchange object
   */
  private boolean processFlowControl(Exchange exchange) {
    if (exchange == null) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot check data flow policy. Exchange object is null.");
      }
      return false;
    }

    // Strict policy: If no PDP is available, block every checked data flow
    PDP pdp = rm.getPdp();
    if (pdp == null) {
      LOG.error("PDP is not available, aborting...");
      return false;
    }

    String source = (String) exchange.getProperty("lastDestination");
    if (source == null) {
      var routeNode = node.getParent();
      while (!(routeNode instanceof RouteDefinition)) {
        routeNode = node.getParent();
      }
      source = ((RouteDefinition) routeNode).getInput().toString();
    }
    String destination = node.toString();
    exchange.setProperty("lastDestination", destination);

    if (LOG.isTraceEnabled()) {
      LOG.trace("{} -> {}", source, destination);
    }

    /*
     * TODO:
     * Nodes currently have no properties or capabilities. They should be retrieved from
     * a) either the prolog knowledge base (a respective query must be created)
     * b) or from service meta data provided by the ConnectionManagerService(?)
     */
    ServiceNode sourceNode = new ServiceNode(source, null, null);
    ServiceNode destNode = new ServiceNode(destination, null, null);

    // Call PDP to transform labels and decide whether to forward the Exchange
    applyLabelTransformation(pdp.requestTranformations(sourceNode), exchange);
    PolicyDecision decision =
        pdp.requestDecision(
            new DecisionRequest(sourceNode, destNode, exchange.getProperties(), null));

    switch (decision.getDecision()) {
      case ALLOW:
        // forward the Exchange
        return true;
      case DENY:
      default:
        if (LOG.isWarnEnabled()) {
          LOG.warn(
              "Exchange blocked by data flow policy. Source: {}, Target: {}", sourceNode, destNode);
        }
        exchange.setException(new Exception("Exchange blocked by data flow policy"));
        return false;
    }

    // TODO: Obligation Implementation
  }

  /**
   * Removes and adds labels to an exchange object.
   *
   * @param requestTransformations The request transformations (label changes) to be performed
   * @param exchange Exchange processed
   */
  @SuppressWarnings("unchecked")
  private void applyLabelTransformation(
      TransformationDecision requestTransformations, Exchange exchange) {
    Set<String> labels =
        (Set<String>)
            exchange.getProperties().computeIfAbsent(PDP.LABELS_KEY, k -> new HashSet<String>());

    // Remove labels from exchange
    labels.removeAll(requestTransformations.getLabelsToRemove());

    // Add labels to exchange
    labels.addAll(requestTransformations.getLabelsToAdd());
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (processFlowControl(exchange)) {
      target.process(exchange);
    }
  }

  @Override
  public boolean process(Exchange exchange, AsyncCallback callback) {
    if (processFlowControl(exchange)) {
      try {
        target.process(exchange);
        callback.done(true);
        return true;
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
    callback.done(false);
    return false;
  }

  @Override
  public CompletableFuture<Exchange> processAsync(Exchange exchange) {
    try {
      target.process(exchange);
      return CompletableFuture.completedFuture(exchange);
    } catch (Exception x) {
      return CompletableFuture.failedFuture(x);
    }
  }
}
