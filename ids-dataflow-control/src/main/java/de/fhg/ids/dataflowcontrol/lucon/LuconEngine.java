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
package de.fhg.ids.dataflowcontrol.lucon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Theory;
import de.fhg.aisec.ids.api.router.CounterExample;
import de.fhg.aisec.ids.api.router.RouteVerificationProof;

/**
 * LUCON (Logic based Usage Control) policy decision engine.
 * 
 * This engine uses tuProlog as a logic language implementation to answer policy
 * decision requests.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class LuconEngine {
	private static final Logger LOG = LoggerFactory.getLogger(LuconEngine.class);
	
	// A Prolog query to compute a path from X to Y in a graph of statements (= a route)
	private static final String QUERY_ROUTE_VERIFICATION = "path(X, Y), entrynode(X), stmt(Y, _).";
	private Prolog p;

	/**
	 * Create a new LuconEngine which writes to a given output stream.
	 * 
	 * @param out
	 *            OutputStream to write Prolog engine outputs to or null if
	 *            output should not printed.
	 */
	public LuconEngine(OutputStream out) {
		p = new Prolog();

		// Add some listeners for logging/debugging
		p.addExceptionListener(ex -> LOG.error("Exception in Prolog reasoning: " + ex.getMsg()));
		p.addQueryListener(q -> LOG.trace("Prolog query " + q.getSolveInfo().getQuery().toString()));
		p.addSpyListener(l -> LOG.trace(l.getMsg() + " " + l.getSource()));
		p.addWarningListener(w -> {
			if (!w.getMsg().contains("The predicate false/0 is unknown"))
				LOG.warn(w.getMsg());
		});
		p.addOutputListener(l -> {
			if (out != null) {
				try {
					out.write(l.getMsg().getBytes());
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}

	public void setSpy(boolean spy) {
		p.setSpy(spy);
	}

	/**
	 * Loads a policy in form of a prolog theory.
	 * 
	 * Existing policies will be overwritten.
	 * 
	 * @param is
	 * @throws InvalidTheoryException  Syntax error in Prolog document
	 * @throws IOException			   I/O error reading from stream
	 */
	public void loadPolicy(InputStream is) throws InvalidTheoryException, IOException {
		Theory t = new Theory(is);
		LOG.debug("Loading theory: " + t.toString());
		p.setTheory(t);
	}

	public List<SolveInfo> query(String query, boolean findAll) throws NoMoreSolutionException, MalformedGoalException {
		return query(p, query, findAll);
	}

	private List<SolveInfo> query(Prolog engine, String query, boolean findAll)	throws NoMoreSolutionException, MalformedGoalException {
		List<SolveInfo> result = new ArrayList<>();
		SolveInfo solution = engine.solve(query);
		while (solution.isSuccess()) {
			result.add(solution);
			if (findAll && engine.hasOpenAlternatives()) {
				solution = engine.solveNext();
			} else {
				break;
			}
		}
		engine.solveEnd();
		return result;
	}

	public String getTheory() {
		return p.getTheory().toString();
	}

	public String getTheoryAsJSON() {
		return p.getTheory().toJSON();
	}

	/**
	 * Returns "true" if the given route is valid under all policies or returns
	 * a set of counterexamples.
	 * 
	 * @param id Route id
	 * @param routePl
	 *            The route, represented as Prolog clauses
	 * @return A list of counterexamples which violate the rule or empty, if no
	 *         route violates the policy.
	 */
	public RouteVerificationProof proofInvalidRoute(String id, String routePl) {
		// JS->ML: Hier wird eine Camel-Route (in Prolog) gegen eine Policy (auch in Prolog) evaluiert. Es ist gut möglich, dass hier noch Fehler drin sin. 
		// The proof object we will return
		RouteVerificationProof proof = new RouteVerificationProof(id);
		
		// Just for information: save the query we used to generate the proof
		proof.setQuery(QUERY_ROUTE_VERIFICATION);
		
		try {
			// Get policy as prolog, add Camel route and init new Prolog engine with combined theory
			Theory t = p.getTheory();
			t.append(new Theory(routePl));
			Prolog newP = new Prolog();
			newP.setTheory(t);
			
			List<CounterExample> ces = new ArrayList<>();
			List<String> currentSteps = new ArrayList<>();

			// Counterexamples are printed to stdout. Fetch them from there.
			newP.addOutputListener(outEvent -> {
				String msg = outEvent.getMsg();
				if ("END TRACE".equals(msg.trim())) {
					// Deep copy into counterexample
					CounterExample ce = new CounterExample();
					currentSteps.forEach(ce::addStep);
					ces.add(ce);
					currentSteps.clear();
				} else if (msg.contains("reason: ")) {
					proof.setExplanation(cleanupProofReason(msg));
				} else {
					if (msg!=null && !"".equals(msg.trim())) {
						currentSteps.add(cleanupProofStep(msg));
					}
				}
			});
			System.out.println("-------------------------");
			System.out.println(t.toString());
			System.out.println("-------------------------");

			// Generate the proof (=run query)
			List<SolveInfo> result = query(newP, QUERY_ROUTE_VERIFICATION, true);
			
			// If a result has been found, this means there is at least one counterexample of a path in a route that violates a policy
			if (!result.isEmpty() && result.get(0).isSuccess()) {
				proof.setCounterexamples(ces);
				proof.setValid(false);
			}
		} catch (InvalidTheoryException | NoMoreSolutionException | MalformedGoalException e) {
			LOG.error(e.getMessage(), e);
		}
		return proof;
	}

	/**
	 * Turns this <code>['reason: service ',stmt_5,' receives label(s) ',[path_B]]</code> into this <code>Service stmt_5 receives forbidden label(s) path_B</code>.
	 * @param msg
	 * @return
	 */
	private String cleanupProofReason(String msg) {
		if (msg==null) {
			return null;
		}
		
		StringBuilder out = new StringBuilder();
		
		// Remove brackets
		if (msg.charAt(0)=='[') {
			int start = 1;
			int end = msg.length()-1;
			if (msg.endsWith("]]")) {
				end--;
			}
			msg = msg.substring(start, end);
		}
				
		// remove strings
		try {
			String[] parts = msg.split(",");
			out.append("service ");
			out.append(parts[1]);
			out.append(" may receive label(s) ");
			out.append(parts[3]);
			out.append(". This is forbidden by rule ");
			out.append(parts[5]);
		} catch (ArrayIndexOutOfBoundsException e) {
			LOG.error("Unexpected: cannot parse proof explanation " + msg, e);
		}
		
		return out.toString();
	}

	/**
	 * Turns this <code>[stmt_2,[label_A,from_entrypoint]]</code> into this <code>stmt_2 : [label_A,from_entrypoint]</code>.
	 * @param msg
	 * @return
	 */
	private String cleanupProofStep(String msg) {
		if (msg==null) {
			return null;
		}
		
		char[] in = msg.toCharArray();
		StringBuilder out = new StringBuilder();
		
		boolean killedFirstBracket = false;
		boolean killedComma = false;
		boolean killedLastBracket = false;
		
		for (int i=0;i<in.length;i++) {
			if (!killedFirstBracket && in[i]=='[') {
				killedFirstBracket = true;
			} else if (!killedComma && in[i]==',') {
				killedComma = true;
				out.append(" receives message labelled ");
			} else if (!killedLastBracket && in[i]==']') {
				killedLastBracket = true; 
			} else {
				out.append(in[i]);
			}
		}
		
		return out.toString();
	}
}