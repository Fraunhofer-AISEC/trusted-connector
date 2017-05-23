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
import alice.tuprolog.NoSolutionException;
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
	
	public void loadPolicy(InputStream is) throws InvalidTheoryException, IOException {
		// Load policy (= prolog "theory")
		p.setTheory(new Theory(is));
	}
	
	public List<SolveInfo> query(String query, boolean findAll) throws NoMoreSolutionException, MalformedGoalException, NoSolutionException {
		List<SolveInfo> result = new ArrayList<>();
		SolveInfo solution = p.solve(query);
		while (solution.isSuccess()) {
			result.add(solution);
			if (findAll & p.hasOpenAlternatives()) {
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
