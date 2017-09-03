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
	Prolog p;
	
	public LuconEngine(OutputStream out) {
		p = new Prolog();

		// Add some listeners for logging/debugging
		p.addExceptionListener(ex -> LOG.error("Exception in Prolog reasoning: " + ex.getMsg()));
		p.addQueryListener(q -> LOG.trace("Prolog query " + q.getSolveInfo().getQuery().toString()));
		p.addSpyListener(l -> LOG.trace(l.getMsg() + " " + l.getSource()));
		p.addWarningListener(w -> {if (!w.getMsg().contains("The predicate false/0 is unknown")) LOG.warn(w.getMsg());});
		p.addOutputListener(l -> { 
			if (out!=null) {
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
	 * @throws InvalidTheoryException
	 * @throws IOException
	 */
	public void loadPolicy(InputStream is) throws InvalidTheoryException, IOException {
		Theory t = new Theory(is);
		LOG.debug("Loading theory: " + t.toString());
		p.setTheory(t);
	}
	
	public List<SolveInfo> query(String query, boolean findAll) throws NoMoreSolutionException, MalformedGoalException {
		List<SolveInfo> result = new ArrayList<>();
		SolveInfo solution = p.solve(query);
		while (solution.isSuccess()) {
			result.add(solution);
			if (findAll && p.hasOpenAlternatives()) {
				solution = p.solveNext();
			} else {
				break;
			}
		}
		p.solveEnd();
		return result;
	}
	
	public String getTheory() {
		return p.getTheory().toString();
	}

	public String getTheoryAsJSON() {
		return p.getTheory().toJSON();
	}
}
