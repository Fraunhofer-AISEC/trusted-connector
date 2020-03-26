/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.dataflowcontrol;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import com.google.common.collect.Sets;
import de.fhg.aisec.ids.api.policy.*;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteVerificationProof;
import de.fhg.aisec.ids.dataflowcontrol.lucon.LuconEngine;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for the LUCON policy engine.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class LuconEngineTest {
  // Solving Towers of Hanoi in only two lines. Prolog FTW!
  private static final String HANOI_THEORY =
      "move(1,X,Y,_) :- "
          + "write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n"
          + "move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ";

  // A random but syntactically correct policy.
  private static final String EXAMPLE_POLICY =
      "\n"
          + "%%%%%%%% Rules %%%%%%%%%%%%\n"
          + "rule(denyAll).\n"
          + "rule_priority(denyAll, 0).\n"
          + "has_decision(denyAll, drop).\n"
          + "receives_label(denyAll).\n"
          + "has_target(denyAll, serviceAll).\n"
          + "\n"
          + "rule(allowRule).\n"
          + "rule_priority(allowRule, 1).\n"
          + "has_decision(allowRule, allow).\n"
          + "receives_label(allowRule).\n"
          + "has_target(allowRule, hiveMqttBrokerService).\n"
          + "has_target(allowRule, anonymizerService).\n"
          + "has_target(allowRule, loggerService).\n"
          + "has_target(allowRule, hadoopClustersService).\n"
          + "has_target(allowRule, testQueueService).\n"
          + "\n"
          + "rule(deleteAfterOneMonth).\n"
          + "rule_priority(deleteAfterOneMonth, 1).\n"
          + "has_decision(deleteAfterOneMonth, allow).\n"
          + "receives_label(deleteAfterOneMonth) :- label(private).\n"
          + "has_target(deleteAfterOneMonth, service78096644).\n"
          + "has_obligation(deleteAfterOneMonth, obl1709554620).\n"
          + "% generated service\n"
          + "service(service78096644).\n"
          + "has_endpoint(service78096644, \"hdfs.*\").\n"
          + "% generated obligation\n"
          + "requires_prerequisite(obl1709554620, delete_after_days(30)).\n"
          + "has_alternativedecision(obl1709554620, drop).\n"
          + "\n"
          + "rule(anotherRule).\n"
          + "rule_priority(anotherRule, 1).\n"
          + "has_target(anotherRule, testQueueService).\n"
          + "receives_label(anotherRule) :- label(private).\n"
          + "has_decision(anotherRule, drop).\n"
          + "\n"
          + "%%%%%%%%%%%% Services %%%%%%%%%%%%\n"
          + "service(serviceAll).\n"
          + "has_endpoint(serviceAll,'.*').\n"
          + "\n"
          + "service(hiveMqttBrokerService).\n"
          + "creates_label(hiveMqttBrokerService, labelone).\n"
          + "creates_label(hiveMqttBrokerService, private).\n"
          + "removes_label(hiveMqttBrokerService, labeltwo).\n"
          + "has_endpoint(hiveMqttBrokerService, \"^paho:.*?tcp://broker.hivemq.com:1883.*\").\n"
          + "has_property(hiveMqttBrokerService, type, public).\n"
          + "\n"
          + "service(anonymizerService).\n"
          + "has_endpoint(anonymizerService, \".*anonymizer.*\").\n"
          + "has_property(anonymizerService, myProp, anonymize('surname', 'name')).\n"
          + "\n"
          + "service(loggerService).\n"
          + "has_endpoint(loggerService, \"^log.*\").\n"
          + "\n"
          + "service(hadoopClustersService).\n"
          + "has_endpoint(hadoopClustersService, \"^hdfs://.*\").\n"
          + "has_capability(hadoopClustersService, deletion).\n"
          + "has_property(hadoopClustersService, anonymizes, anonymize('surname', 'name')).\n"
          + "\n"
          + "service(testQueueService).\n"
          + "has_endpoint(testQueueService, \"^amqp:.*?:test\").";

  // Policy with extended labels, i.e. "purpose(green)"
  private static final String EXTENDED_LABELS_POLICY =
      ""
          + "%%%%%%%% Rules %%%%%%%%%%%%\n"
          + "rule(denyAll).\n"
          + "rule_priority(denyAll, 0).\n"
          + "has_decision(denyAll, drop).\n"
          + "receives_label(denyAll).\n"
          + "has_target(denyAll, serviceAll).\n"
          + "\n"
          + "rule(demo).\n"
          + "rule_priority(demo, 1).\n"
          + "has_target(demo, service473016340).\n"
          + "service(service473016340).\n"
          + "has_endpoint(service473016340,\"(ahc|ahc-ws|cxf|cxfbean|cxfrs)://.*\").\n"
          + "receives_label(demo) :- label(purpose(green)).\n" // Note that Prolog does not support
          // nested predicates.
          + "has_decision(demo, allow).\n"
          + "\n"
          + "%%%%% Services %%%%%%%%%%%%\n"
          + "service(serviceAll).\n"
          + "has_endpoint(serviceAll,'.*').\n"
          + "creates_label(serviceAll, purpose(green)).\n"
          + "\n"
          + "service(sanitizedata).\n"
          + "has_endpoint(sanitizedata, \"^bean://SanitizerBean.*\").\n"
          + "creates_label(sanitizedata, public).\n"
          + "removes_label(sanitizedata, private).\n";

  // Route from LUCON paper with path searching logic
  private static final String VERIFIABLE_ROUTE =
      "%\n"
          + "% (C) Julian Schütte, Fraunhofer AISEC, 2017\n"
          + "%\n"
          + "% Demonstration of model checking a message route against a usage control policy\n"
          + "%\n"
          + "% Message Route definition\n"
          + "%\n"
          + "%       hiveMqttBroker       \n"
          + "%       /     \\     \n"
          + "%  logger    anonymizer  \n"
          + "%       \\     /     \n"
          + "%       hadoopClusters       \n"
          + "%         |          \n"
          + "%       testQueue       \n"
          + "entrynode(hiveMqttBroker).\n"
          + "stmt(hiveMqttBroker).\n"
          + "has_action(hiveMqttBroker, \"paho:something:tcp://broker.hivemq.com:1883/anywhere\").\n"
          + "stmt(logger).\n"
          + "has_action(logger, \"log\").\n"
          + "stmt(anonymizer).\n"
          + "has_action(anonymizer, \"hello_anonymizer_world\").\n"
          + "stmt(hadoopClusters).\n"
          + "has_action(hadoopClusters, \"hdfs://myCluser\").\n"
          + "stmt(testQueue).\n"
          + "has_action(testQueue, \"amqp:testQueue:test\").\n"
          + "\n"
          + "succ(hiveMqttBroker, logger).\n"
          + "succ(hiveMqttBroker, anonymizer).\n"
          + "succ(logger, hadoopClusters).\n"
          + "succ(anonymizer, hadoopClusters).\n"
          + "succ(hadoopClusters, testQueue).\n"
          + "\n";

  /**
   * Loading a valid Prolog theory should not fail.
   *
   * @throws InvalidTheoryException If invalid theory is encountered
   */
  @Test
  public void testLoadingTheoryGood() throws InvalidTheoryException {
    LuconEngine e = new LuconEngine(null);
    e.loadPolicy(HANOI_THEORY);
    String json = e.getTheoryAsJSON();
    assertTrue(json.startsWith("{\"theory\":\"move(1,X,Y,"));
    String prolog = e.getTheory();
    assertTrue(prolog.trim().startsWith("move(1,X,Y"));
  }

  /** Loading an invalid Prolog theory is expected to throw an exception. */
  @Test
  public void testLoadingTheoryNotGood() {
    LuconEngine e = new LuconEngine(System.out);
    try {
      e.loadPolicy("This is invalid");
    } catch (InvalidTheoryException ex) {
      return; // Expected
    }
    fail("Could load invalid theory without exception");
  }

  /**
   * Solve a simple Prolog puzzle.
   *
   * @throws InvalidTheoryException If invalid theory is encountered
   */
  @Test
  public void testSimplePrologQuery() throws InvalidTheoryException {
    LuconEngine e = new LuconEngine(System.out);
    e.loadPolicy(HANOI_THEORY);
    try {
      List<SolveInfo> solutions = e.query("move(3,left,right,center). ", true);
      assertEquals(1, solutions.size());
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
   * @throws InvalidTheoryException If invalid theory is encountered
   */
  @Test
  public void testSolve2() throws InvalidTheoryException {
    LuconEngine e = new LuconEngine(System.out);
    e.loadPolicy(EXAMPLE_POLICY);
    try {
      List<SolveInfo> solutions =
          e.query("has_endpoint(X,Y),regex_match(Y, \"hdfs://myendpoint\").", true);
      assertNotNull(solutions);
      assertEquals(3, solutions.size());
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
   */
  @Test
  public void testPolicyDecision() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();
    pdp.loadPolicy(EXAMPLE_POLICY);

    // Simple message context with nonsense attributes
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("some_message_key", "some_message_value");
    attributes.put(PDP.LABELS_KEY, Sets.newHashSet("private"));

    // Simple source and dest nodes
    ServiceNode source = new ServiceNode("seda:test_source", null, null);
    ServiceNode dest = new ServiceNode("hdfs://some_url", null, null);

    PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
    assertEquals(Decision.ALLOW, dec.getDecision());

    //    // Check obligation
    //    assertEquals(3, dec.getObligations().size());
    //    Obligation obl = dec.getObligations().get(0);
    //    assertEquals("delete_after_days(30)", obl.getAction());
  }

  /**
   * Test if the correct policy decisions are taken for a (very) simple route and an example policy.
   */
  @Test
  public void testPolicyDecisionWithExtendedLabels() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();
    pdp.loadPolicy(EXTENDED_LABELS_POLICY);

    // Simple message context with nonsense attributes
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("some_message_key", "some_message_value");
    attributes.put(PDP.LABELS_KEY, Sets.newHashSet("purpose(green)"));

    // Simple source and dest nodes
    ServiceNode source = new ServiceNode("seda:test_source", null, null);
    ServiceNode dest = new ServiceNode("ahc://some_url", null, null);

    PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
    assertEquals(Decision.ALLOW, dec.getDecision());

    // Check obligation
    assertEquals(0, dec.getObligations().size());
  }

  /** List all rules of the currently loaded policy. */
  @Test
  public void testListRules() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();

    // Without any policy, we expect an empty list of rules
    List<String> emptyList = pdp.listRules();
    assertNotNull(emptyList);
    assertTrue(emptyList.isEmpty());

    // Load a policy
    pdp.loadPolicy(EXAMPLE_POLICY);

    // We now expect 3 rules
    List<String> rules = pdp.listRules();
    assertNotNull(rules);
    assertEquals(4, rules.size());
    assertTrue(rules.contains("deleteAfterOneMonth"));
    assertTrue(rules.contains("anotherRule"));
  }

  @Test
  public void testTransformationsMatch() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();
    pdp.loadPolicy(EXAMPLE_POLICY);
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
  public void testTransformationsNomatch() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();
    pdp.loadPolicy(EXAMPLE_POLICY);
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
   * @throws Exception If something fails
   */
  @Test
  public void testVerifyRoute() throws Exception {
    // Create RouteManager returning VERIFIABLE_ROUTE
    RouteManager rm = mock(RouteManager.class);
    System.out.println("------ ROUTE ----------");
    System.out.println(VERIFIABLE_ROUTE);
    when(rm.getRouteAsProlog(anyString())).thenReturn(VERIFIABLE_ROUTE);

    // Create policy decision point and attach to route manager
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    pdp.loadPolicies();

    // Manually inject routemanager into PDP
    Field f1 = pdp.getClass().getDeclaredField("routeManager");
    f1.setAccessible(true);
    f1.set(pdp, rm);

    // Load policy
    pdp.loadPolicy(EXAMPLE_POLICY);

    // Verify VERIFIABLE_ROUTE against EXAMPLE_POLICY
    RouteVerificationProof proof = pdp.verifyRoute("mockId");
    System.out.println("------ Proof follows ----------");
    System.out.println(proof != null ? proof.toString() : null);
    assertNotNull(proof);
    assertFalse(proof.isValid());
    //		assertTrue(proof.toString().contains("Service testQueueService may receive messages labeled
    // [private], " + "which is forbidden by rule \"anotherRule\"."));
    System.out.println("##### PROBLEM #####");
    System.out.println(proof.toString());
    assertTrue(
        proof
            .toString()
            .contains(
                "Service testQueueService may receive messages, which is forbidden by rule \"anotherRule\"."));
    assertNotNull(proof.getCounterExamples());
  }

  @Test
  @Ignore("Not a regular unit test; for evaluating runtime performance.")
  public void testPerformanceEvaluationScaleRules() {
    for (int i = 10; i <= 5000; i += 10) {
      // Load n test rules into PDP
      String theory = generateRules(i);
      PolicyDecisionPoint pdp = new PolicyDecisionPoint();
      pdp.loadPolicies();
      long start = System.nanoTime();
      pdp.loadPolicy(theory);
      long stop = System.nanoTime();
      long loadTime = (stop - start);

      // Simple message context with nonsense attributes
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("some_message_key", "some_message_value");

      // Simple source and dest nodes
      ServiceNode source = new ServiceNode("seda:test_source", null, null);
      ServiceNode dest = new ServiceNode("hdfs://some_url", null, null);

      start = System.nanoTime();
      PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
      stop = System.nanoTime();
      long queryTime = stop - start;

      System.out.println(i + "\t\t" + loadTime + "\t\t" + queryTime);
      assertEquals(Decision.ALLOW, dec.getDecision());
    }
  }

  @Test
  @Ignore("Not a regular unit test; for evaluating runtime performance.")
  public void testPerformanceEvaluationScaleLabels() {
    for (int i = 0; i <= 5000; i += 10) {
      // Load n test rules into PDP
      String theory = generateLabels(i);
      PolicyDecisionPoint pdp = new PolicyDecisionPoint();
      pdp.loadPolicies();
      long start = System.nanoTime();
      pdp.loadPolicy(theory);
      long stop = System.nanoTime();
      long loadTime = (stop - start);

      // Simple message context with nonsense attributes
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("some_message_key", "some_message_value");

      // Simple source and dest nodes
      ServiceNode source = new ServiceNode("seda:test_source", null, null);
      ServiceNode dest = new ServiceNode("hdfs://some_url", null, null);

      start = System.nanoTime();
      PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
      stop = System.nanoTime();
      long queryTime = stop - start;

      System.out.println(i + "\t\t" + loadTime + "\t\t" + queryTime);
      assertEquals(Decision.ALLOW, dec.getDecision());
    }
  }

  @Test
  @Ignore("Not a regular unit test; for evaluating runtime performance.")
  public void testmemoryEvaluationScaleRules() {
    for (int i = 10; i <= 5000; i += 10) {
      // Load n test rules into PDP
      String theory = generateRules(i);
      PolicyDecisionPoint pdp = new PolicyDecisionPoint();
      pdp.loadPolicies();
      pdp.loadPolicy(theory);

      // Simple message context with nonsense attributes
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("some_message_key", "some_message_value");

      // Simple source and dest nodes
      ServiceNode source = new ServiceNode("seda:test_source", null, null);
      ServiceNode dest = new ServiceNode("hdfs://some_url", null, null);

      System.gc();
      System.gc();
      System.gc(); // Empty level 1- & 2-LRUs.
      long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
      long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      System.gc();
      System.gc();
      System.gc(); // Empty level 1- & 2-LRUs.
      long memoryAfterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      System.out.println(
          i + "\t\t" + (memoryAfter - memoryBefore) + "\t\t" + (memoryAfterGC - memoryBefore));
      assertEquals(Decision.ALLOW, dec.getDecision());
    }
  }

  @Test
  @Ignore("Not a regular unit test; for evaluating runtime performance.")
  public void testmemoryEvaluationScaleLabels() {
    for (int i = 10; i <= 5000; i += 10) {
      // Load n test rules into PDP
      String theory = generateLabels(i);
      PolicyDecisionPoint pdp = new PolicyDecisionPoint();
      pdp.loadPolicies();
      pdp.loadPolicy(theory);

      // Simple message context with nonsense attributes
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("some_message_key", "some_message_value");

      // Simple source and dest nodes
      ServiceNode source = new ServiceNode("seda:test_source", null, null);
      ServiceNode dest = new ServiceNode("hdfs://some_url", null, null);

      System.gc();
      System.gc();
      System.gc(); // Empty level 1- & 2-LRUs.
      long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      PolicyDecision dec = pdp.requestDecision(new DecisionRequest(source, dest, attributes, null));
      long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      System.gc();
      System.gc();
      System.gc(); // Empty level 1- & 2-LRUs.
      long memoryAfterGC = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      System.out.println(
          i + "\t\t" + (memoryAfter - memoryBefore) + "\t\t" + (memoryAfterGC - memoryBefore));
      assertEquals(Decision.ALLOW, dec.getDecision());
    }
  }

  @Test
  public void testRulePriorities() {
    PolicyDecisionPoint pdp = new PolicyDecisionPoint();
    // Load test policy w/ two rules
    InputStream policy = this.getClass().getClassLoader().getResourceAsStream("policy-example.pl");
    assertNotNull(policy);
    pdp.loadPolicy(new Scanner(policy, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next());
    assertEquals(2, pdp.listRules().size());

    // Test order of specification in rule file
    String ruleThree = pdp.listRules().get(0);
    assertEquals("testRulePrioThree", ruleThree);
    String ruleTwo = pdp.listRules().get(1);
    assertEquals("testRulePrioTwo", ruleTwo);

    // FALL-THROUGH: Test fall-through decision (no msg label matches)
    ServiceNode from = new ServiceNode("IAmMatchedByRuleThreeOnly", null, null);
    ServiceNode to = new ServiceNode("hdfs://IAmMatchedByBothRules", null, null);
    Map<String, Object> msgCtx = new HashMap<>();
    Map<String, Object> envCtx = new HashMap<>();
    DecisionRequest req = new DecisionRequest(from, to, msgCtx, envCtx);
    PolicyDecision dec = pdp.requestDecision(req);
    assertNotNull(dec);
    Decision d = dec.getDecision();
    assertEquals(Decision.DENY, d);
    assertEquals("No matching rule", dec.getReason());

    // FALL-THROUGH: presence of a label "public" (w/o any specific value) does not yet trigger
    // testRulePrioTwo, because label "filtered" is required in addition.
    from = new ServiceNode("IAmMatchedByRuleThreeOnly", null, null);
    to = new ServiceNode("hdfs://IAmMatchedByBothRules", null, null);
    msgCtx = new HashMap<>();
    msgCtx.put(PDP.LABELS_KEY, Sets.newHashSet("public"));
    envCtx = new HashMap<>();
    req = new DecisionRequest(from, to, msgCtx, envCtx);
    dec = pdp.requestDecision(req);
    assertNotNull(dec);
    d = dec.getDecision();
    assertEquals(Decision.DENY, d);
    assertEquals("No matching rule", dec.getReason());

    // testRulePrioTwo: now we have labels "public" AND "filtered" set, which makes testRulePrioTwo
    // match
    from = new ServiceNode("IAmMatchedByRuleThreeOnly", null, null);
    to = new ServiceNode("hdfs://IAmMatchedByBothRules", null, null);
    msgCtx = new HashMap<>();
    msgCtx.put(PDP.LABELS_KEY, Sets.newHashSet("public", "filtered"));
    envCtx = new HashMap<>();
    req = new DecisionRequest(from, to, msgCtx, envCtx);
    dec = pdp.requestDecision(req);
    assertNotNull(dec);
    d = dec.getDecision();
    assertEquals(Decision.ALLOW, d);
    assertEquals("testRulePrioTwo", dec.getReason());

    // testRulePrioTwo: "public" AND "filtered" makes testRulePrioTwo match. Additional labels do
    // not harm
    from = new ServiceNode("IAmMatchedByRuleThreeOnly", null, null);
    to = new ServiceNode("hdfs://IAmMatchedByBothRules", null, null);
    msgCtx = new HashMap<>();
    msgCtx.put(PDP.LABELS_KEY, Sets.newHashSet("public", "unusedlabel"));
    envCtx = new HashMap<>();
    req = new DecisionRequest(from, to, msgCtx, envCtx);
    dec = pdp.requestDecision(req);
    assertNotNull(dec);
    d = dec.getDecision();
    assertEquals(Decision.DENY, d);
    assertEquals("No matching rule", dec.getReason());

    // testRulePrioTwo: labels "public", "filtered", "private" will trigger testRulePrioOne and
    // testRulePrioTwo. Rule with higher prio wins.
    from = new ServiceNode("IAmMatchedByRuleThreeOnly", null, null);
    to = new ServiceNode("hdfs://IAmMatchedByBothRules", null, null);
    msgCtx = new HashMap<>();
    msgCtx.put(PDP.LABELS_KEY, Sets.newHashSet("public", "unusedlabel", "private"));
    envCtx = new HashMap<>();
    req = new DecisionRequest(from, to, msgCtx, envCtx);
    dec = pdp.requestDecision(req);
    assertNotNull(dec);
    d = dec.getDecision();
    assertEquals(Decision.DENY, d);
    assertEquals("testRulePrioThree", dec.getReason());
  }

  /**
   * Generates n random rules matching a target endpoint (given as regex).
   *
   * <p>All rules will take an "allow" decision.
   *
   * @param n The number of rules to generate
   * @return The rules as String
   */
  private String generateRules(int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n; i++) {
      String ruleName = "testRule" + i;
      String targetName = "testTarget" + i;
      String labelName = "label" + i;
      sb.append("rule(").append(ruleName).append(").\n");
      sb.append("has_decision(").append(ruleName).append(", allow).\n");
      sb.append("has_alternativedecision(").append(ruleName).append(", allow).\n");
      sb.append("receives_label(").append(ruleName).append(").\n");
      sb.append("has_target(").append(ruleName).append(", ").append(targetName).append(").\n");
      sb.append("has_obligation(")
          .append(ruleName)
          .append(", testObligation")
          .append(i)
          .append(").\n");
      sb.append("service(").append(targetName).append(").\n");
      sb.append("has_endpoint(").append(targetName).append(", \".*\").\n");
      sb.append("creates_label(").append(targetName).append(", ").append(labelName).append(").\n");
      sb.append("removes_label(").append(targetName).append(", ").append(labelName).append(").\n");
    }
    return sb.toString();
  }

  private String generateLabels(int n) {
    StringBuilder sb = new StringBuilder();
    sb.append(generateRules(50));
    for (int i = 0; i < n; i++) {
      sb.append("creates_label(testTarget1, labelX").append(i).append(").\n");
      sb.append("removes_label(testTarget1, labelX").append(i).append(").\n");
    }
    return sb.toString();
  }
}
