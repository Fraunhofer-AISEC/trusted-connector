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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.Obligation;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.aisec.ids.api.policy.ServiceNode;
import de.fhg.aisec.ids.api.policy.TransformationDecision;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteVerificationProof;
import de.fhg.ids.dataflowcontrol.PolicyDecisionPoint;

/**
 * Unit tests for the LUCON policy engine.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class LuconEngineTest {
	// Solving Towers of Hanoi in only two lines. Prolog FTW!
	private final static String HANOI_THEORY = 	"move(1,X,Y,_) :- " +
            "write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n" +
            "move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ";

	// A random but syntactically correct policy.
	private final static String EXAMPLE_POLICY =
			"\n" +
			"%%%%%%%% Rules %%%%%%%%%%%%\n" +
			"rule(denyAll).\n" +
			"rule_priority(denyAll, 0).\n" +
			"has_decision(denyAll, drop).\n" +
			"receives_label(denyAll) :- any.\n" +
			"has_target(denyAll, serviceAll).\n" +
			"\n" +
			"rule(allowRule).\n" +
			"rule_priority(allowRule, 1).\n" +
			"has_decision(allowRule, allow).\n" +
			"receives_label(allowRule) :- any.\n" +
			"has_target(allowRule, hiveMqttBrokerService).\n" +
			"has_target(allowRule, anonymizerService).\n" +
			"has_target(allowRule, loggerService).\n" +
			"has_target(allowRule, hadoopClustersService).\n" +
			"has_target(allowRule, testQueueService).\n" +
			"\n" +
			"rule(deleteAfterOneMonth).\n" +
			"rule_priority(deleteAfterOneMonth, 1).\n" +
			"has_decision(deleteAfterOneMonth, allow).\n" +
			"receives_label(deleteAfterOneMonth) :- private.\n" +
			"has_target(deleteAfterOneMonth, service78096644).\n" +
			"has_obligation(deleteAfterOneMonth, obl1709554620).\n" +
			"% generated service\n" +
			"service(service78096644).\n" +
			"has_endpoint(service78096644, \"hdfs.*\").\n" +
			"% generated obligation\n" +
			"requires_prerequisite(obl1709554620, delete_after_days(30)).\n" +
			"has_alternativedecision(obl1709554620, drop).\n" +
			"\n" +
			"rule(anotherRule).\n" +
			"rule_priority(anotherRule, 1).\n" +
			"has_target(anotherRule, testQueueService).\n" +
			"receives_label(anotherRule) :- private.\n" +
			"has_decision(anotherRule, drop).\n" +
			"\n" +
			"%%%%%%%%%%%% Services %%%%%%%%%%%%\n" +
			"service(serviceAll).\n" +
			"has_endpoint(serviceAll,'.*').\n" +
			"creates_label(serviceAll,[]).\n" +
			"removes_label(serviceAll,[]).\n" +
			"\n" +
			"service(hiveMqttBrokerService).\n" +
			"creates_label(hiveMqttBrokerService, [labelone, private]).\n" +
			"removes_label(hiveMqttBrokerService, [labeltwo]).\n" +
			"has_endpoint(hiveMqttBrokerService, \"^paho:.*?tcp://broker.hivemq.com:1883.*\").\n" +
			"has_property(hiveMqttBrokerService, type, public).\n" +
			"\n" +
			"service(anonymizerService).\n" +
			"has_endpoint(anonymizerService, \".*anonymizer.*\").\n" +
			"has_property(anonymizerService, myProp, anonymize('surname', 'name')).\n" +
			"removes_label(anonymizerService, []).\n" +
			"creates_label(anonymizerService, []).\n" +
			"\n" +
			"service(loggerService).\n" +
			"has_endpoint(loggerService, \"^log.*\").\n" +
			"removes_label(loggerService, []).\n" +
			"creates_label(loggerService, []).\n" +
			"\n" +
			"service(hadoopClustersService).\n" +
			"has_endpoint(hadoopClustersService, \"^hdfs://.*\").\n" +
			"has_capability(hadoopClustersService, deletion).\n" +
			"has_property(hadoopClustersService, anonymizes, anonymize('surname', 'name')).\n" +
			"removes_label(hadoopClustersService, []).\n" +
			"creates_label(hadoopClustersService, []).\n" +
			"\n" +
			"service(testQueueService).\n" +
			"has_endpoint(testQueueService, \"^amqp:.*?:test\").\n" +
			"removes_label(testQueueService, []).\n" +
			"creates_label(testQueueService, []).";
	
	// Route from LUCON paper with path searching logic
	public static final String VERIFIABLE_ROUTE = "%\n" + 
			"% (C) Julian Schütte, Fraunhofer AISEC, 2017\n" + 
			"%\n" + 
			"% Demonstration of model checking a message route against a usage control policy\n" + 
			"%\n" + 
			"% Message Route definition\n" + 
			"%\n" + 
			"%       hiveMqttBroker       \n" + 
			"%       /     \\     \n" + 
			"%  logger    anonymizer  \n" + 
			"%       \\     /     \n" + 
			"%       hadoopClusters       \n" + 
			"%         |          \n" + 
			"%       testQueue       \n" + 
			"entrynode(hiveMqttBroker).\n" +
			"stmt(hiveMqttBroker).\n" +
			"has_action(hiveMqttBroker, \"paho:something:tcp://broker.hivemq.com:1883/anywhere\").\n" +
			"stmt(logger).\n" +
			"has_action(logger, \"log\").\n" +
			"stmt(anonymizer).\n" +
			"has_action(anonymizer, \"hello_anonymizer_world\").\n" +
			"stmt(hadoopClusters).\n" +
			"has_action(hadoopClusters, \"hdfs://myCluser\").\n" +
			"stmt(testQueue).\n" +
			"has_action(testQueue, \"amqp:testQueue:test\").\n" +
			"\n" + 
			"succ(hiveMqttBroker, logger).\n" + 
			"succ(hiveMqttBroker, anonymizer).\n" + 
			"succ(logger, hadoopClusters).\n" + 
			"succ(anonymizer, hadoopClusters).\n" + 
			"succ(hadoopClusters, testQueue).\n" +
			"\n";
	
	
	/**
	 * Loading a valid Prolog theory should not fail.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 */
	@Test
	public void testLoadingTheoryGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(null);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
		String json = e.getTheoryAsJSON();
		assertTrue(json.startsWith("{\"theory\":\"move(1,X,Y,"));
		String prolog = e.getTheory();
		assertTrue(prolog.trim().startsWith("move(1,X,Y"));
	}

	/**
	 * Loading an invalid Prolog theory is expected to throw an exception.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 */
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
	
	/**
	 * Solve a simple Prolog puzzle.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 * @throws NoMoreSolutionException
	 */
	@Test
	public void testSolve1() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
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
	
	/**
	 * Run some simple queries against an actual policy.
	 * 
	 * @throws InvalidTheoryException
	 * @throws IOException
	 * @throws NoMoreSolutionException
	 */
	@Test
	public void testSolve2() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("has_endpoint(X,Y),regex_match(Y, \"hdfs://myendpoint\").",
					true);
			assertNotNull(solutions);
			assertEquals(3,solutions.size());
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
	
	/**
	 * Test if the correct policy decisions are taken for a (very) simple route and an example policy.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testPolicyDecision() throws IOException {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// Simple message context with nonsense attributes
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("some_message_key", "some_message_value");
		attributes.put(PDP.LABEL_PREFIX+"1", "private");
		
		// Simple source and dest nodes
		ServiceNode source = new ServiceNode("seda:test_source", null, null);
		ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
		
		PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
		assertEquals(Decision.ALLOW, dec.getDecision());
		
		// Check obligation
		assertEquals(3, dec.getObligations().size());
		Obligation obl = dec.getObligations().get(0);
		assertEquals("delete_after_days(30)", obl.getAction());
	}

	/**
	 * List all rules of the currently loaded policy.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testListRules() throws IOException {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		
		// Without any policy, we expect an empty list of rules
		List<String> emptyList = pdp.listRules();
		assertNotNull(emptyList);
		assertTrue(emptyList.isEmpty());
				
		// Load a policy
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// We now expect 3 rules
		List<String> rules = pdp.listRules();
		assertNotNull(rules);
		assertEquals(4, rules.size());
		assertTrue(rules.contains("deleteAfterOneMonth"));
		assertTrue(rules.contains("anotherRule"));
	}
	
	@Test
	public void testTransformationsMatch() throws IOException {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		ServiceNode node = new ServiceNode("paho:tcp://broker.hivemq.com:1883/blablubb", null, null);
		TransformationDecision trans = pdp.requestTranformations(node);
		
		assertNotNull(trans);
		assertNotNull(trans.getLabelsToAdd());
		assertNotNull(trans.getLabelsToRemove());
		
		assertEquals(2, trans.getLabelsToAdd().size());
		assertEquals(1, trans.getLabelsToRemove().size());

		assertTrue(trans.getLabelsToAdd().contains("labelone"));
		assertTrue(trans.getLabelsToRemove().contains("labeltwo"));
	}

	@Test
	public void testTransformationsNomatch() throws IOException {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		ServiceNode node = new ServiceNode("someendpointwhichisnotmatchedbypolicy", null, null);
		TransformationDecision trans = pdp.requestTranformations(node);
		
		assertNotNull(trans);
		assertNotNull(trans.getLabelsToAdd());
		assertNotNull(trans.getLabelsToRemove());
		
		assertEquals(0, trans.getLabelsToAdd().size());
		assertEquals(0, trans.getLabelsToRemove().size());
	}
	
	/**
	 * Tests the generation of a proof that a route matches a policy.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVerifyRoute() throws Exception {
		// Create RouteManager returning VERIFIABLE_ROUTE
		RouteManager rm = mock(RouteManager.class);
		when(rm.getRouteAsProlog(anyString())).thenReturn(VERIFIABLE_ROUTE);
		
		// Create policy decision point and attach to route manager
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.bindRouteManager(rm);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// Verify VERIFIABLE_ROUTE against EXAMPLE_POLICY
		RouteVerificationProof proof = pdp.verifyRoute("mockId");
		System.out.println("------ Proof follows ----------");
		System.out.println(proof.toString());
		assertNotNull(proof);
		assertFalse(proof.isValid());
//		assertTrue(proof.toString().contains("Service testQueueService may receive messages labeled [private], " + "which is forbidden by rule \"anotherRule\"."));
		assertTrue(proof.toString().contains("Service testQueueService may receive messages, which is forbidden by rule \"anotherRule\"."));
		assertNotNull(proof.getCounterExamples());
	}
	
	@Test
	@Ignore	// Not a regular unit test For evaluating runtime performance.
	public void testPerformanceEvaluationScaleRules() throws Exception {
		for (int i=10;i<=5000;i+=10) {
			// Load n test rules into PDP 
			String theory = generateRules(i, ".*");
			PolicyDecisionPoint pdp = new PolicyDecisionPoint();
			pdp.activate(null);
			long start = System.nanoTime();
			pdp.loadPolicy(new ByteArrayInputStream(theory.getBytes()));
			long stop = System.nanoTime();
			long loadTime = (stop-start);
			
			// Simple message context with nonsense attributes
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("some_message_key", "some_message_value");
			
			// Simple source and dest nodes
			ServiceNode source = new ServiceNode("seda:test_source", null, null);
			ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
			
			start = System.nanoTime();
			PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
			stop = System.nanoTime();
			long queryTime = stop-start;
			
			System.out.println(i + "\t\t" + loadTime + "\t\t" + queryTime);
			assertEquals(Decision.ALLOW, dec.getDecision());
		}
	}
	
	@Test
	@Ignore	// Not a regular unit test For evaluating runtime performance.
	public void testPerformanceEvaluationScaleLabels() throws Exception {
		for (int i=0;i<=5000;i+=10) {
			// Load n test rules into PDP 
			String theory = generateLabels(i, ".*");
			PolicyDecisionPoint pdp = new PolicyDecisionPoint();
			pdp.activate(null);
			long start = System.nanoTime();
			pdp.loadPolicy(new ByteArrayInputStream(theory.getBytes()));
			long stop = System.nanoTime();
			long loadTime = (stop-start);
			
			// Simple message context with nonsense attributes
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("some_message_key", "some_message_value");
			
			// Simple source and dest nodes
			ServiceNode source = new ServiceNode("seda:test_source", null, null);
			ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
			
			start = System.nanoTime();
			PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
			stop = System.nanoTime();
			long queryTime = stop-start;
			
			System.out.println(i + "\t\t" + loadTime + "\t\t" + queryTime);
			assertEquals(Decision.ALLOW, dec.getDecision());
		}
	}

	@Test
	@Ignore	// Not a regular unit test For evaluating runtime performance.
	public void testmemoryEvaluationScaleRules() throws Exception {
		for (int i=10;i<=5000;i+=10) {
			// Load n test rules into PDP 
			String theory = generateRules(i, ".*");
			PolicyDecisionPoint pdp = new PolicyDecisionPoint();
			pdp.activate(null);
			pdp.loadPolicy(new ByteArrayInputStream(theory.getBytes()));
			
			// Simple message context with nonsense attributes
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("some_message_key", "some_message_value");
			
			// Simple source and dest nodes
			ServiceNode source = new ServiceNode("seda:test_source", null, null);
			ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
			
			System.gc(); System.gc(); System.gc(); // Empty level 1- & 2-LRUs.
			long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.gc(); System.gc(); System.gc(); // Empty level 1- & 2-LRUs.
			long memoryAfterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

			
			System.out.println(i + "\t\t" + (memoryAfter-memoryBefore) + "\t\t" + (memoryAfterGC-memoryBefore));
			assertEquals(Decision.ALLOW, dec.getDecision());
		}
	}
	
	@Test
	@Ignore	// Not a regular unit test For evaluating runtime performance.
	public void testmemoryEvaluationScaleLabels() throws Exception {
		for (int i=10;i<=5000;i+=10) {
			// Load n test rules into PDP 
			String theory = generateLabels(i, ".*");
			PolicyDecisionPoint pdp = new PolicyDecisionPoint();
			pdp.activate(null);
			pdp.loadPolicy(new ByteArrayInputStream(theory.getBytes()));
			
			// Simple message context with nonsense attributes
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("some_message_key", "some_message_value");
			
			// Simple source and dest nodes
			ServiceNode source = new ServiceNode("seda:test_source", null, null);
			ServiceNode dest= new ServiceNode("hdfs://some_url", null, null);
			
			System.gc(); System.gc(); System.gc(); // Empty level 1- & 2-LRUs.
			long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.gc(); System.gc(); System.gc(); // Empty level 1- & 2-LRUs.
			long memoryAfterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

			
			System.out.println(i + "\t\t" + (memoryAfter-memoryBefore) + "\t\t" + (memoryAfterGC-memoryBefore));
			assertEquals(Decision.ALLOW, dec.getDecision());
		}
	}
	/**
	 * Generates n random rules matching a target endpoint (given as regex).
	 * 
	 * All rules will take an "allow" decision.
	 * 
	 * @param n
	 * @return
	 */
	private String generateRules(int n, String targetEndpointRegex) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<n;i++) {
			sb.append("rule(testRule"+i+").\n");
			sb.append("has_decision(testRule"+i+", allow).\n");
			sb.append("has_alternativedecision(testRule"+i+", allow).\n");
			sb.append("receives_label(testRule"+i+", any).\n");
			sb.append("has_target(testRule"+i+", testTarget"+i+").\n");
			sb.append("has_obligation(testRule"+i+", testObligation"+i+").\n");
			sb.append("service(testTarget"+i+").\n");
			sb.append("has_endpoint(testTarget"+i+", \""+targetEndpointRegex+"\").\n");
			sb.append("creates_label(testTarget"+i+", label"+i+").\n");
			sb.append("removes_label(testTarget"+i+", label"+i+").\n");
		}
		return sb.toString();
	}
	
	private String generateLabels(int n, String targetEndpointRegex) {
		StringBuilder sb = new StringBuilder();
		sb.append(generateRules(50, targetEndpointRegex));
		for (int i=0;i<n;i++) {
			sb.append("creates_label(testTarget1, labelX"+i+").\n");
			sb.append("removes_label(testTarget1, labelX"+i+").\n");
		}
		return sb.toString();
	}
}
