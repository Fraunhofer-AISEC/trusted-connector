package de.fhg.ids.dataflowcontrol.lucon;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;

public class LuconEngineTest {
	// Solving Towers of Hanoi in only two lines. Prolog FTW!
	private final static String THEORY = 	"move(1,X,Y,_) :-  write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n" +
											"move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ";
	
	
	@Test
	public void testLoadingTheoryGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(null);
		e.loadPolicy(new ByteArrayInputStream(THEORY.getBytes()));
		String json = e.getTheoryAsJSON();
		assertTrue(json.startsWith("{\"theory\":\"move(1,X,Y,"));
		String prolog = e.getTheory();
		assertTrue(prolog.trim().startsWith("move(1,X,Y"));
	}

	@Test
	public void testLoadingTheoryNotGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(System.out);
		try {
			e.loadPolicy(new ByteArrayInputStream("This is invalid".getBytes()));
		} catch (InvalidTheoryException ex) {
			return;	// Expected
		}
		fail("Could load invalid theory without exception");
	}
	
	@Test
	public void testSolve() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(THEORY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("move(3,left,right,center). ", true);
			assertTrue(solutions.size()==1);
			for (SolveInfo solution : solutions) {
				System.out.println(solution.getSolution().toString());
				System.out.println(solution.hasOpenAlternatives());
				
				System.out.println(solution.isSuccess());
			}
		} catch (MalformedGoalException | NoSolutionException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}

}
