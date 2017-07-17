/*-
 * ========================LICENSE_START=================================
 * LUCON Data Flow Policy Engine
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
package de.fhg.ids.dataflowcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.PrologException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Var;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.Obligation;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.aisec.ids.api.policy.ServiceNode;
import de.fhg.aisec.ids.api.policy.TransformationDecision;
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
		
	/**
	 * Result of query will be:
	 *
	 * Target			Decision	Alternative		Obligation
	 * 	hadoopClusters	D			drop			(delete_after_days(_1354),_1354<30)	1
	 *	hiveMqttBroker	drop		Alt				A
	 * @param msgLabels 
	 */
	private String createDecisionQuery(ServiceNode target, Set<String> msgLabels) {			
		StringBuilder sb = new StringBuilder();
		sb.append("rule(_X), has_target(_X, T), ");
		sb.append("has_endpoint(T, EP), ");
		sb.append("regex(EP, \""+target.getEndpoint()+"\", true), ");
		for (String label: msgLabels) {
			sb.append("receives_label(T, "+label+"), ");
		}
		if (target.getCapabilties().size() + target.getProperties().size() > 0) {
			sb.append("(");
		}
		for (String cap: target.getCapabilties()) {
			sb.append("(has_capability(T, \""+cap+"\"); ");
		}
		for (String prop: target.getProperties()) {
			sb.append("has_property(T, \""+prop+"\"), ");
		}
		if (target.getCapabilties().size() + target.getProperties().size() > 0) {
			sb.append("), ");
		}
		sb.append("(has_decision(_X, D); (has_obligation(_X, _O), has_alternativedecision(_O, Alt), requires_prerequisite(_O, A))).");
		return sb.toString();
	}
	
	private String createTransformationQuery(ServiceNode target) {			
		StringBuilder sb = new StringBuilder();
		sb.append("service(T), ");
		// FIXME Unfinished. Return set of additions and removals here!
		return sb.toString();
	}

	@Activate
	public void activate(ComponentContext ctx) {
		if (this.engine == null) {
			this.engine = new LuconEngine(System.out);
		}
	}

	@Override
	public TransformationDecision requestTranformations(ServiceNode lastServiceNode) {
		// Query prolog for labels to remove or add from message
		String query = this.createTransformationQuery(lastServiceNode);
		LOG.info("QUERY: " + query);
		try {
			List<SolveInfo> solveInfo = this.engine.query(query, false);
		} catch (NoMoreSolutionException | MalformedGoalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new TransformationDecision();
	}
	
	@Override
	public PolicyDecision requestDecision(DecisionRequest req) {
		// TODO move to own method, make this more efficient
		Set<String> msgLabels = new HashSet<>();
		if (req.getMessageCtx().get("labels")!=null) {
			for (String label : req.getMessageCtx().get("labels").split(",")) {
				if (!"".equals(label)) {
					msgLabels.add(label);
				}
			}
		}
		LOG.debug("Decision requested " + req.getFrom() + " -> " + req.getTo());
		PolicyDecision dec = new PolicyDecision();
		dec.setDecision(Decision.ALLOW); // Default value
		dec.setReason("Not yet ready for productive use!");
		try {
			// Query Prolog engine for a policy decision
			long startTime = System.nanoTime();
			String query = this.createDecisionQuery(req.getTo(), msgLabels);
			LOG.info("QUERY: " + query);
			List<SolveInfo> solveInfo = this.engine.query(query, false);
			long time = System.nanoTime() - startTime;
			LOG.info("Policy decision took " + time + " nanos");
						
			// Just for debugging
			if (LOG.isDebugEnabled()) {
				debug(solveInfo);
			}
			
			// If there is no matching rule, allow by default
			if (solveInfo.isEmpty()) {
				return dec;
			}
			
			// Get some obligation, if any TODO incorrect. Merge obligations of all matching rules. Currently we are just pulling out "any"
			List<Var> vars = solveInfo.get(0).getBindingVars();
			Optional<Var> alt = vars.stream().filter(v -> "Alt".equals(v.getName())).findAny();
			Optional<Var> action = vars.stream().filter(v -> "A".equals(v.getName())).findAny();
			if (action.isPresent()) {
				Obligation o = new Obligation();				
				o.setAction(action.get().getTerm().toString());
				if (alt.isPresent() && "drop".equals(alt.get().getTerm().toString())) {
					o.setAlternativeDecision(Decision.DENY);
				}
				dec.setObligation(o);
				dec.setDecision(Decision.ALLOW);
			}
			
			Optional<Var> decision = vars.stream().filter(v -> "D".equals(v.getName()) && v.isBound()).findAny();
			if (decision.isPresent()) {
				if ("drop".equals(decision.get().getTerm().toString())) {
					dec.setDecision(Decision.DENY);
				}
			}
		} catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException e) {
			LOG.error(e.getMessage(), e);
			dec.setDecision(Decision.DENY);
		}
		return dec;
	}

	/**
	 * Just for debugging: Print query solution to DEBUG out.
	 * 
	 * @param solveInfo
	 * @throws NoSolutionException
	 */
	private void debug(List<SolveInfo> solveInfo) throws NoSolutionException {
		for (SolveInfo i: solveInfo) {
			if (i.isSuccess()) {
				List<Var> vars = i.getBindingVars();
				vars.stream().forEach(v -> LOG.debug(v.getName() + ":" + v.getTerm() + " bound: " + v.isBound()));
			}
		}
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
	public List<String> listRules() {
		ArrayList<String> result = new ArrayList<>();
		try {
			List<SolveInfo> rules = this.engine.query("rule(X).", true);
			for (SolveInfo r : rules) {
				result.add(r.getVarValue("X").toString());
			}
		} catch (PrologException e) {
			LOG.error("Prolog error while retrieving rules " + e.getMessage(), e);
		}
		return result;
	}

	@Override
	public String getPolicy() {
		return this.engine.getTheory();
	}
}
