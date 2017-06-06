package de.fhg.ids.dataflowcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Var;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
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
	private static final String QUERY_GET_ALL_RULES = "rule(R), has_target(R, Service), has_property(Service, Prop), has_obligation(R, Obl), requires_action(Obl, Act).";
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
		dec.setReason("Not yet ready for productive use!");
		try {
			// Query Prolog engine for a policy decision
			long startTime = System.nanoTime();
			List<SolveInfo> solveInfo = this.engine.query(QUERY_GET_ALL_RULES, false);
			long time = System.nanoTime() - startTime;
			LOG.info("Policy decision took " + time/1000000 + " ms");
						
			// Just for debugging
			if (LOG.isDebugEnabled()) {
				for (SolveInfo i: solveInfo) {
					if (i.isSuccess()) {
						List<Var> vars = i.getBindingVars();
						vars.stream().forEach(v -> LOG.debug(v.getName() + ":" + v.getTerm() + " bound: " + v.isBound()));
					}
				}
			}
			
			// If there is no matching rule, deny by default
			if (solveInfo.isEmpty()) {
				dec.setDecision(Decision.DENY);
				return dec;
			}
			
			// Get some obligation, if any TODO merge obligations of all matching rules
			List<Var> vars = solveInfo.get(0).getBindingVars();
			Optional<Var> obl = vars.stream().filter(v -> v.getName().equals("Obl")).findAny();
			if (obl.isPresent()) {
				dec.setObligation(obl.get().getTerm().toString());
				dec.setDecision(Decision.ALLOW);
			}
		} catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException e) {
			LOG.error(e.getMessage(), e);
			dec.setDecision(Decision.DENY);
		}
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
		} catch (InvalidTheoryException e) {
			LOG.error("Error in " + e.line + " " + e.pos + ": " + e.clause + ": " + e.getMessage(), e);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public String getPolicy() {
		return this.engine.getTheory();
	}
}