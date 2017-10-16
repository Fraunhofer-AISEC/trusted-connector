/*-
 * ========================LICENSE_START=================================
 * IDS Container Manager
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.management.InstrumentationProcessor;
import org.apache.camel.processor.LogProcessor;
import org.apache.camel.processor.SendProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.ServiceNode;
import de.fhg.aisec.ids.api.policy.TransformationDecision;

/**
 * 
 * @author Mathias Morbitzer (mathias.morbitzer@aisec.fraunhofer.de)
 *
 */
public class PolicyEnforcementPoint implements AsyncProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PolicyEnforcementPoint.class);  
    private Processor target;
	private RouteManagerService rm;
    
    public PolicyEnforcementPoint(Processor target, RouteManagerService rm) {
    	this.target = target;
    	this.rm = rm;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
    	// Check if environment is usable as expected
    	if (target==null || exchange==null || !(target instanceof InstrumentationProcessor)) {
			LOG.warn("Cannot check data flow policy. Null or no InstrumentationProcessor");
			return;
		}
		
		if (rm==null || rm.getPdp()==null) {
    		target.process(exchange);
			return;
		}

		// Log statements may pass through immediately
    	if (((InstrumentationProcessor) target).getProcessor() instanceof LogProcessor) {
    		target.process(exchange);
    		return;
    	}
    	
    	// We expect a SendProcessor to retrieve the endpoint URL from
		if (! (((InstrumentationProcessor) target).getProcessor() instanceof SendProcessor) ) {
				LOG.warn("Not a SendProcessor or LogProcessor. Skipping. " + ((InstrumentationProcessor) target).getProcessor().getClass());
				target.process(exchange);
				return;
		}
		
		// Figure out where the message comes from and where it should go to
		SendProcessor sendprocessor = (SendProcessor) ((InstrumentationProcessor) target).getProcessor();
		String destination = sendprocessor.getEndpoint().getEndpointUri();
		String source = exchange.getFromEndpoint().getEndpointUri();
		
		LOG.info("START: Check if label(s) exist in allow rules for destination " + destination);
		
		// If no PDP is available at runtime -> skip
		PDP pdp = rm.getPdp();
		if (pdp == null) {
			target.process(exchange);
			return;
		}
		
		/* TODO Nodes currently have no capabilities and properties. They should be retrieved from
		*  a) either the prolog knowledge base (a respective query must be created)
		*  b) from service meta data provided by the ConnectionManagerService(?)
		*/
		ServiceNode sourceNode = new ServiceNode(source, null, null);
		ServiceNode destNode = new ServiceNode(destination, null, null);
		
		// Call PDP to transform labels and decide whether we may forward the Exchange
		applyLabelTransformation(pdp.requestTranformations(sourceNode), exchange);
		PolicyDecision decision = pdp.requestDecision(new DecisionRequest(sourceNode, destNode, exchange.getProperties(), null));

		switch (decision.getDecision()) {
		case DON_T_CARE:
		case ALLOW:
			// forward the Exchange
			target.process(exchange);
			break;
		case DENY:
		default:
			LOG.info("Exchange blocked by data flow policy. Target was " + exchange.getFromEndpoint().getEndpointUri());
			exchange.setException(new Exception("Exchange blocked by policy"));
		}
		
		// TODO Run obligation

    }

	/**
	 * Removes and adds labels to an exchange object.
	 * 
	 * @param requestTranformations
	 * @param msg
	 */
    private void applyLabelTransformation(TransformationDecision requestTranformations, Exchange msg) {
		Map<String, Object> props = msg.getProperties();
		
		// Remove labels from exchange
		for (Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith(PDP.LABEL_PREFIX)) {
				String label = e.getKey().replaceFirst(PDP.LABEL_PREFIX, "");
				if (requestTranformations.getLabelsToRemove().contains(label)) {
					msg.removeProperty(label);
				}
			}
		}
		
		// Add labels to exchange
		for (String label : requestTranformations.getLabelsToAdd()) {
			msg.setProperty(PDP.LABEL_PREFIX+label, "");
		}
	}

	@Override
	public boolean process(Exchange exchange, AsyncCallback callback) {
		try {
			process(exchange);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}
}
