/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
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
package de.fhg.aisec.ids.rm;

import de.fhg.aisec.ids.api.policy.*;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.processor.LogProcessor;
import org.apache.camel.processor.SendProcessor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyEnforcementPoint implements AsyncProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcementPoint.class);
  private Processor target, effectiveTarget;
  private RouteManagerService rm;

  public PolicyEnforcementPoint(@Nullable Processor target, @Nullable RouteManagerService rm) {
    this.target = target;
    if (target instanceof DelegateAsyncProcessor) {
      this.effectiveTarget = ((DelegateAsyncProcessor) target).getProcessor();
    } else {
      this.effectiveTarget = target;
    }
    this.rm = rm;
  }

  /**
   * The method performs flow control and calls Exchange.setException() when necessary
   *
   * @param exchange The exchange object to check
   * @return Whether target.process() is to be called for this exchange object
   */
  public boolean processFlowControl(Exchange exchange) {
    if (exchange == null) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot check data flow policy. Exchange object is null.");
      }
      return false;
    }

    if (target == null) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Cannot check data flow policy. The target is null.");
      }
      return false;
    }

    // Log statements may pass through immediately
    if (effectiveTarget instanceof LogProcessor) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Passing through LogProcessor...");
      }
      return true;
    }

    // We expect a SendProcessor to retrieve the endpoint URL from
    if (!(effectiveTarget instanceof SendProcessor)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Not a SendProcessor. Ignoring {}", target.getClass().getName());
      }
      return true;
    }

    // Strict policy: If no PDP is available, block every checked data flow
    PDP pdp;
    if (rm == null) {
      LOG.error("RouteManager is not available, aborting...");
      return false;
    }
    pdp = rm.getPdp();
    if (pdp == null) {
      LOG.error("PDP is not available, aborting...");
      return false;
    }

    String source = exchange.getFromEndpoint().getEndpointUri();
    String destination = ((SendProcessor) effectiveTarget).getEndpoint().getEndpointUri();

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
   * @param requestTransformations The request transformations to be performed
   * @param msg Message to process
   */
  private void applyLabelTransformation(
      TransformationDecision requestTransformations, Exchange msg) {
    // Remove labels from exchange
    requestTransformations
        .getLabelsToRemove()
        .stream()
        .map(l -> PDP.LABEL_PREFIX + l)
        .forEach(msg::removeProperty);

    // Add labels to exchange
    requestTransformations
        .getLabelsToAdd()
        .stream()
        .map(l -> PDP.LABEL_PREFIX + l)
        .forEach(l -> msg.setProperty(l, ""));
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
}
