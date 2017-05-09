package de.fhg.ids.dataflowcontrol;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;

@Component(enabled=true, immediate=true, name="ids-dataflow-control")
public class PolicyDecisionPoint implements PDP {
	private static final Logger LOG = LoggerFactory.getLogger(PolicyDecisionPoint.class);

	@Override
	public PolicyDecision requestDecision(DecisionRequest req) {
		LOG.debug("Decision requested " + req.getFrom() + " -> " + req.getTo() + " : " + String.join(", ", req.getMessageCtx().get("labels")));
		PolicyDecision dec = new PolicyDecision();
		dec.setDecision(PolicyDecision.Decision.ALLOW);
		dec.setReason("Dummy");
		return dec;
	}

	@Override
	public void clearAllCaches() {
		// Nothing to do here at the moment		
	}
}
