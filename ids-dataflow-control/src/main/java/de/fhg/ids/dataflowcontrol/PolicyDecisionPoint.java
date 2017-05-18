package de.fhg.ids.dataflowcontrol;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alice.tuprolog.InvalidTheoryException;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.ids.dataflowcontrol.lucon.LuconEngine;

/**
 * servicefactory=false is the default and actually not required. But we want to make
 * clear that this is a singleton, i.e. there will only be one instance of
 * PolicyDecisionPoint within the whole runtime.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(enabled = true, immediate = true, name = "ids-dataflow-control", servicefactory = false)
public class PolicyDecisionPoint implements PDP, PAP {
	private static final Logger LOG = LoggerFactory.getLogger(PolicyDecisionPoint.class);
	private LuconEngine engine;
	
	@Activate
	public void activate(ComponentContext ctx) {
		if (this.engine == null) {
			this.engine = new LuconEngine(System.out);
		}
	}

	@Override
	public PolicyDecision requestDecision(DecisionRequest req) {
		LOG.debug("Decision requested " + req.getFrom() + " -> " + req.getTo() + " : " + String.join(", ", req.getMessageCtx().get("labels")));
		PolicyDecision dec = new PolicyDecision();
		dec.setDecision(PolicyDecision.Decision.ALLOW);
		dec.setReason("Dummy");
// TODO Call Prolog here
//		try {
//			this.engine.query("rule(_).", false);
//		} catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException e) {
//			e.printStackTrace();
//		}
		return dec;
	}

	@Override
	public void clearAllCaches() {
		// Nothing to do here at the moment
	}

	@Override
	public void loadPolicy(InputStream is) {
		try {
			this.engine.loadPolicy(is);
		} catch (InvalidTheoryException | IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public String getPolicy() {
		return this.engine.getTheory();
	}
}
