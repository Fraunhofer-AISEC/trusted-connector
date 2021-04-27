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
package de.fhg.aisec.ids.dataflowcontrol

import alice.tuprolog.exceptions.InvalidTheoryException
import alice.tuprolog.exceptions.MalformedGoalException
import alice.tuprolog.exceptions.NoSolutionException
import de.fhg.aisec.ids.api.policy.DecisionRequest
import de.fhg.aisec.ids.api.policy.PolicyDecision
import de.fhg.aisec.ids.api.policy.ServiceNode
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.dataflowcontrol.lucon.LuconEngine
import java.nio.charset.StandardCharsets
import java.util.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/**
 * Unit tests for the LUCON policy engine.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
class LuconEngineTest {
    /**
     * Loading a valid Prolog theory should not fail.
     *
     * @throws InvalidTheoryException If invalid theory is encountered
     */
    @Test
    @Throws(InvalidTheoryException::class)
    fun testLoadingTheoryGood() {
        val e = LuconEngine(null)
        e.loadPolicy(HANOI_THEORY)
        val prolog = e.theory
        Assert.assertTrue(prolog.trim { it <= ' ' }.startsWith("move(1,X,Y"))
    }

    /** Loading an invalid Prolog theory is expected to throw an exception. */
    @Test
    fun testLoadingTheoryNotGood() {
        val e = LuconEngine(System.out)
        try {
            e.loadPolicy("This is invalid")
        } catch (ex: InvalidTheoryException) {
            return // Expected
        }
        Assert.fail("Could load invalid theory without exception")
    }

    /**
     * Solve a simple Prolog puzzle.
     *
     * @throws InvalidTheoryException If invalid theory is encountered
     */
    @Test
    @Throws(InvalidTheoryException::class)
    fun testSimplePrologQuery() {
        val e = LuconEngine(System.out)
        e.loadPolicy(HANOI_THEORY)
        try {
            val solutions = e.query("move(3,left,right,center). ", true)
            Assert.assertEquals(1, solutions.size.toLong())
            for (solution in solutions) {
                println(solution.solution.toString())
                println(solution.hasOpenAlternatives())
                println(solution.isSuccess)
            }
        } catch (e1: MalformedGoalException) {
            e1.printStackTrace()
            Assert.fail(e1.message)
        } catch (e1: NoSolutionException) {
            e1.printStackTrace()
            Assert.fail(e1.message)
        }
    }

    /**
     * Run some simple queries against an actual policy.
     *
     * @throws InvalidTheoryException If invalid theory is encountered
     */
    @Test
    @Throws(InvalidTheoryException::class)
    fun testSolve2() {
        val e = LuconEngine(System.out)
        e.loadPolicy(EXAMPLE_POLICY)
        try {
            val solutions =
                e.query("has_endpoint(X,Y),regex_match(Y, \"hdfs://myendpoint\").", true)
            Assert.assertNotNull(solutions)
            Assert.assertEquals(3, solutions.size.toLong())
            for (solution in solutions) {
                println(solution.solution.toString())
                println(solution.hasOpenAlternatives())
                println(solution.isSuccess)
            }
        } catch (e1: MalformedGoalException) {
            e1.printStackTrace()
            Assert.fail(e1.message)
        } catch (e1: NoSolutionException) {
            e1.printStackTrace()
            Assert.fail(e1.message)
        }
    }

    /**
     * Test if the correct policy decisions are taken for a (very) simple route and an example
     * policy.
     */
    @Test
    fun testPolicyDecision() {
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()
        pdp.loadPolicy(EXAMPLE_POLICY)

        // Simple source and dest nodes
        val source = ServiceNode("seda:test_source", null, null)
        val dest = ServiceNode("hdfs://some_url", null, null)
        val dec = pdp.requestDecision(DecisionRequest(source, dest, setOf("private"), null))
        Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)

        //    // Check obligation
        //    assertEquals(3, dec.getObligations().size());
        //    Obligation obl = dec.getObligations().get(0);
        //    assertEquals("delete_after_days(30)", obl.getAction());
    }

    /**
     * Test if the correct policy decisions are taken for a (very) simple route and an example
     * policy.
     */
    @Test
    fun testPolicyDecisionWithExtendedLabels() {
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()
        pdp.loadPolicy(EXTENDED_LABELS_POLICY)

        // Simple source and dest nodes
        val source = ServiceNode("seda:test_source", null, null)
        val dest = ServiceNode("ahc://some_url", null, null)
        val dec = pdp.requestDecision(DecisionRequest(source, dest, setOf("purpose(green)"), null))
        Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)

        // Check obligation
        Assert.assertEquals(0, dec.obligations.size.toLong())
    }

    /** List all rules of the currently loaded policy. */
    @Test
    fun testListRules() {
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()

        // Without any policy, we expect an empty list of rules
        val emptyList = pdp.listRules()
        Assert.assertNotNull(emptyList)
        Assert.assertTrue(emptyList.isEmpty())

        // Load a policy
        pdp.loadPolicy(EXAMPLE_POLICY)

        // We now expect 3 rules
        val rules = pdp.listRules()
        Assert.assertNotNull(rules)
        Assert.assertEquals(4, rules.size.toLong())
        Assert.assertTrue(rules.contains("deleteAfterOneMonth"))
        Assert.assertTrue(rules.contains("anotherRule"))
    }

    @Test
    fun testTransformationsMatch() {
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()
        pdp.loadPolicy(EXAMPLE_POLICY)
        val node = ServiceNode("paho:tcp://broker.hivemq.com:1883/blablubb", null, null)
        val trans = pdp.requestTranformations(node)
        Assert.assertNotNull(trans)
        Assert.assertNotNull(trans.labelsToAdd)
        Assert.assertNotNull(trans.labelsToRemove)
        Assert.assertEquals(2, trans.labelsToAdd.size.toLong())
        Assert.assertEquals(1, trans.labelsToRemove.size.toLong())
        Assert.assertTrue(trans.labelsToAdd.contains("labelone"))
        Assert.assertTrue(trans.labelsToRemove.contains("labeltwo"))
    }

    @Test
    fun testTransformationsNomatch() {
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()
        pdp.loadPolicy(EXAMPLE_POLICY)
        val node = ServiceNode("someendpointwhichisnotmatchedbypolicy", null, null)
        val trans = pdp.requestTranformations(node)
        Assert.assertNotNull(trans)
        Assert.assertNotNull(trans.labelsToAdd)
        Assert.assertNotNull(trans.labelsToRemove)
        Assert.assertEquals(0, trans.labelsToAdd.size.toLong())
        Assert.assertEquals(0, trans.labelsToRemove.size.toLong())
    }

    /**
     * Tests the generation of a proof that a route matches a policy.
     *
     * @throws Exception If something fails
     */
    @Test
    @Throws(Exception::class)
    fun testVerifyRoute() {
        // Create RouteManager returning VERIFIABLE_ROUTE
        val rm = Mockito.mock(RouteManager::class.java)
        println("------ ROUTE ----------")
        println(VERIFIABLE_ROUTE)
        Mockito.`when`(rm.getRouteAsProlog(ArgumentMatchers.anyString()))
            .thenReturn(VERIFIABLE_ROUTE)

        // Create policy decision point and attach to route manager
        val pdp = PolicyDecisionPoint()
        pdp.loadPolicies()

        // Manually inject routemanager into PDP
        val f1 = pdp.javaClass.getDeclaredField("routeManager")
        f1.isAccessible = true
        f1[pdp] = rm

        // Load policy
        pdp.loadPolicy(EXAMPLE_POLICY)

        // Verify VERIFIABLE_ROUTE against EXAMPLE_POLICY
        val proof = pdp.verifyRoute("mockId")
        println("------ Proof follows ----------")
        println(proof?.toString())
        Assert.assertNotNull(proof)
        Assert.assertFalse(proof!!.isValid)
        //		assertTrue(proof.toString().contains("Service testQueueService may receive messages
        // labeled
        // [private], " + "which is forbidden by rule \"anotherRule\"."));
        println("##### PROBLEM #####")
        println(proof.toString())
        Assert.assertTrue(
            proof
                .toString()
                .contains(
                    "Service testQueueService may receive messages, which is forbidden by rule \"anotherRule\"."
                )
        )
        Assert.assertNotNull(proof.counterExamples)
    }

    @Test
    @Ignore("Not a regular unit test; for evaluating runtime performance.")
    fun testPerformanceEvaluationScaleRules() {
        var i = 10
        while (i <= 5000) {

            // Load n test rules into PDP
            val theory = generateRules(i)
            val pdp = PolicyDecisionPoint()
            pdp.loadPolicies()
            var start = System.nanoTime()
            pdp.loadPolicy(theory)
            var stop = System.nanoTime()
            val loadTime = stop - start

            // Simple source and dest nodes
            val source = ServiceNode("seda:test_source", null, null)
            val dest = ServiceNode("hdfs://some_url", null, null)
            start = System.nanoTime()
            val dec = pdp.requestDecision(DecisionRequest(source, dest, emptySet(), null))
            stop = System.nanoTime()
            val queryTime = stop - start
            println(i.toString() + "\t\t" + loadTime + "\t\t" + queryTime)
            Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)
            i += 10
        }
    }

    @Test
    @Ignore("Not a regular unit test; for evaluating runtime performance.")
    fun testPerformanceEvaluationScaleLabels() {
        var i = 0
        while (i <= 5000) {

            // Load n test rules into PDP
            val theory = generateLabels(i)
            val pdp = PolicyDecisionPoint()
            pdp.loadPolicies()
            var start = System.nanoTime()
            pdp.loadPolicy(theory)
            var stop = System.nanoTime()
            val loadTime = stop - start

            // Simple source and dest nodes
            val source = ServiceNode("seda:test_source", null, null)
            val dest = ServiceNode("hdfs://some_url", null, null)
            start = System.nanoTime()
            val dec = pdp.requestDecision(DecisionRequest(source, dest, emptySet(), null))
            stop = System.nanoTime()
            val queryTime = stop - start
            println(i.toString() + "\t\t" + loadTime + "\t\t" + queryTime)
            Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)
            i += 10
        }
    }

    @Test
    @Ignore("Not a regular unit test; for evaluating runtime performance.")
    fun testmemoryEvaluationScaleRules() {
        var i = 10
        while (i <= 5000) {

            // Load n test rules into PDP
            val theory = generateRules(i)
            val pdp = PolicyDecisionPoint()
            pdp.loadPolicies()
            pdp.loadPolicy(theory)

            // Simple source and dest nodes
            val source = ServiceNode("seda:test_source", null, null)
            val dest = ServiceNode("hdfs://some_url", null, null)
            System.gc()
            System.gc()
            System.gc() // Empty level 1- & 2-LRUs.
            val memoryBefore =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val dec = pdp.requestDecision(DecisionRequest(source, dest, emptySet(), null))
            val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            System.gc()
            System.gc()
            System.gc() // Empty level 1- & 2-LRUs.
            val memoryAfterGC =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            println(
                i.toString() +
                    "\t\t" +
                    (memoryAfter - memoryBefore) +
                    "\t\t" +
                    (memoryAfterGC - memoryBefore)
            )
            Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)
            i += 10
        }
    }

    @Test
    @Ignore("Not a regular unit test; for evaluating runtime performance.")
    fun testmemoryEvaluationScaleLabels() {
        var i = 10
        while (i <= 5000) {

            // Load n test rules into PDP
            val theory = generateLabels(i)
            val pdp = PolicyDecisionPoint()
            pdp.loadPolicies()
            pdp.loadPolicy(theory)

            // Simple source and dest nodes
            val source = ServiceNode("seda:test_source", null, null)
            val dest = ServiceNode("hdfs://some_url", null, null)
            System.gc()
            System.gc()
            System.gc() // Empty level 1- & 2-LRUs.
            val memoryBefore =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val dec = pdp.requestDecision(DecisionRequest(source, dest, emptySet(), null))
            val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            System.gc()
            System.gc()
            System.gc() // Empty level 1- & 2-LRUs.
            val memoryAfterGC =
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            println(
                i.toString() +
                    "\t\t" +
                    (memoryAfter - memoryBefore) +
                    "\t\t" +
                    (memoryAfterGC - memoryBefore)
            )
            Assert.assertEquals(PolicyDecision.Decision.ALLOW, dec.decision)
            i += 10
        }
    }

    @Test
    fun testRulePriorities() {
        val pdp = PolicyDecisionPoint()
        // Load test policy w/ two rules
        val policy = this.javaClass.classLoader.getResourceAsStream("policy-example.pl")
        Assert.assertNotNull(policy)
        pdp.loadPolicy(Scanner(policy!!, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next())
        Assert.assertEquals(2, pdp.listRules().size.toLong())

        // Test order of specification in rule file
        val ruleThree = pdp.listRules()[0]
        Assert.assertEquals("testRulePrioThree", ruleThree)
        val ruleTwo = pdp.listRules()[1]
        Assert.assertEquals("testRulePrioTwo", ruleTwo)

        // FALL-THROUGH: Test fall-through decision (no msg label matches)
        var from = ServiceNode("IAmMatchedByRuleThreeOnly", null, null)
        var to = ServiceNode("hdfs://IAmMatchedByBothRules", null, null)
        var envCtx: Map<String?, Any?> = HashMap()
        var req = DecisionRequest(from, to, emptySet(), envCtx)
        var dec = pdp.requestDecision(req)
        Assert.assertNotNull(dec)
        var d = dec.decision
        Assert.assertEquals(PolicyDecision.Decision.DENY, d)
        Assert.assertEquals("No matching rule", dec.reason)

        // FALL-THROUGH: presence of a label "public" (w/o any specific value) does not yet trigger
        // testRulePrioTwo, because label "filtered" is required in addition.
        from = ServiceNode("IAmMatchedByRuleThreeOnly", null, null)
        to = ServiceNode("hdfs://IAmMatchedByBothRules", null, null)
        envCtx = HashMap()
        req = DecisionRequest(from, to, setOf("public"), envCtx)
        dec = pdp.requestDecision(req)
        Assert.assertNotNull(dec)
        d = dec.decision
        Assert.assertEquals(PolicyDecision.Decision.DENY, d)
        Assert.assertEquals("No matching rule", dec.reason)

        // testRulePrioTwo: now we have labels "public" AND "filtered" set, which makes
        // testRulePrioTwo
        // match
        from = ServiceNode("IAmMatchedByRuleThreeOnly", null, null)
        to = ServiceNode("hdfs://IAmMatchedByBothRules", null, null)
        envCtx = HashMap()
        req = DecisionRequest(from, to, setOf("public", "filtered"), envCtx)
        dec = pdp.requestDecision(req)
        Assert.assertNotNull(dec)
        d = dec.decision
        Assert.assertEquals(PolicyDecision.Decision.ALLOW, d)
        Assert.assertEquals("testRulePrioTwo", dec.reason)

        // testRulePrioTwo: "public" AND "filtered" makes testRulePrioTwo match. Additional labels
        // do
        // not harm
        from = ServiceNode("IAmMatchedByRuleThreeOnly", null, null)
        to = ServiceNode("hdfs://IAmMatchedByBothRules", null, null)
        envCtx = HashMap()
        req = DecisionRequest(from, to, setOf("public", "unusedlabel"), envCtx)
        dec = pdp.requestDecision(req)
        Assert.assertNotNull(dec)
        d = dec.decision
        Assert.assertEquals(PolicyDecision.Decision.DENY, d)
        Assert.assertEquals("No matching rule", dec.reason)

        // testRulePrioTwo: labels "public", "filtered", "private" will trigger testRulePrioOne and
        // testRulePrioTwo. Rule with higher prio wins.
        from = ServiceNode("IAmMatchedByRuleThreeOnly", null, null)
        to = ServiceNode("hdfs://IAmMatchedByBothRules", null, null)
        envCtx = HashMap()
        req = DecisionRequest(from, to, setOf("public", "unusedlabel", "private"), envCtx)
        dec = pdp.requestDecision(req)
        Assert.assertNotNull(dec)
        d = dec.decision
        Assert.assertEquals(PolicyDecision.Decision.DENY, d)
        Assert.assertEquals("testRulePrioThree", dec.reason)
    }

    /**
     * Generates n random rules matching a target endpoint (given as regex).
     *
     * All rules will take an "allow" decision.
     *
     * @param n The number of rules to generate
     * @return The rules as String
     */
    private fun generateRules(n: Int): String {
        val sb = StringBuilder()
        for (i in 0 until n) {
            val ruleName = "testRule$i"
            val targetName = "testTarget$i"
            val labelName = "label$i"
            sb.append("rule(").append(ruleName).append(").\n")
            sb.append("has_decision(").append(ruleName).append(", allow).\n")
            sb.append("has_alternativedecision(").append(ruleName).append(", allow).\n")
            sb.append("receives_label(").append(ruleName).append(").\n")
            sb.append("has_target(").append(ruleName).append(", ").append(targetName).append(").\n")
            sb.append("has_obligation(")
                .append(ruleName)
                .append(", testObligation")
                .append(i)
                .append(").\n")
            sb.append("service(").append(targetName).append(").\n")
            sb.append("has_endpoint(").append(targetName).append(", \".*\").\n")
            sb.append("creates_label(")
                .append(targetName)
                .append(", ")
                .append(labelName)
                .append(").\n")
            sb.append("removes_label(")
                .append(targetName)
                .append(", ")
                .append(labelName)
                .append(").\n")
        }
        return sb.toString()
    }

    private fun generateLabels(n: Int): String {
        val sb = StringBuilder()
        sb.append(generateRules(50))
        for (i in 0 until n) {
            sb.append("creates_label(testTarget1, labelX").append(i).append(").\n")
            sb.append("removes_label(testTarget1, labelX").append(i).append(").\n")
        }
        return sb.toString()
    }

    companion object {
        // Solving Towers of Hanoi in only two lines. Prolog FTW!
        private const val HANOI_THEORY =
            ("move(1,X,Y,_) :- " +
                "write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n" +
                "move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ")

        // A random but syntactically correct policy.
        private const val EXAMPLE_POLICY =
            ("\n" +
                "%%%%%%%% Rules %%%%%%%%%%%%\n" +
                "rule(denyAll).\n" +
                "rule_priority(denyAll, 0).\n" +
                "has_decision(denyAll, drop).\n" +
                "receives_label(denyAll).\n" +
                "has_target(denyAll, serviceAll).\n" +
                "\n" +
                "rule(allowRule).\n" +
                "rule_priority(allowRule, 1).\n" +
                "has_decision(allowRule, allow).\n" +
                "receives_label(allowRule).\n" +
                "has_target(allowRule, hiveMqttBrokerService).\n" +
                "has_target(allowRule, anonymizerService).\n" +
                "has_target(allowRule, loggerService).\n" +
                "has_target(allowRule, hadoopClustersService).\n" +
                "has_target(allowRule, testQueueService).\n" +
                "\n" +
                "rule(deleteAfterOneMonth).\n" +
                "rule_priority(deleteAfterOneMonth, 1).\n" +
                "has_decision(deleteAfterOneMonth, allow).\n" +
                "receives_label(deleteAfterOneMonth) :- label(private).\n" +
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
                "receives_label(anotherRule) :- label(private).\n" +
                "has_decision(anotherRule, drop).\n" +
                "\n" +
                "%%%%%%%%%%%% Services %%%%%%%%%%%%\n" +
                "service(serviceAll).\n" +
                "has_endpoint(serviceAll,'.*').\n" +
                "\n" +
                "service(hiveMqttBrokerService).\n" +
                "creates_label(hiveMqttBrokerService, labelone).\n" +
                "creates_label(hiveMqttBrokerService, private).\n" +
                "removes_label(hiveMqttBrokerService, labeltwo).\n" +
                "has_endpoint(hiveMqttBrokerService, \"^paho:.*?tcp://broker.hivemq.com:1883.*\").\n" +
                "has_property(hiveMqttBrokerService, type, public).\n" +
                "\n" +
                "service(anonymizerService).\n" +
                "has_endpoint(anonymizerService, \".*anonymizer.*\").\n" +
                "has_property(anonymizerService, myProp, anonymize('surname', 'name')).\n" +
                "\n" +
                "service(loggerService).\n" +
                "has_endpoint(loggerService, \"^log.*\").\n" +
                "\n" +
                "service(hadoopClustersService).\n" +
                "has_endpoint(hadoopClustersService, \"^hdfs://.*\").\n" +
                "has_capability(hadoopClustersService, deletion).\n" +
                "has_property(hadoopClustersService, anonymizes, anonymize('surname', 'name')).\n" +
                "\n" +
                "service(testQueueService).\n" +
                "has_endpoint(testQueueService, \"^amqp:.*?:test\").")

        // Policy with extended labels, i.e. "purpose(green)"
        private const val EXTENDED_LABELS_POLICY =
            ("" +
                "%%%%%%%% Rules %%%%%%%%%%%%\n" +
                "rule(denyAll).\n" +
                "rule_priority(denyAll, 0).\n" +
                "has_decision(denyAll, drop).\n" +
                "receives_label(denyAll).\n" +
                "has_target(denyAll, serviceAll).\n" +
                "\n" +
                "rule(demo).\n" +
                "rule_priority(demo, 1).\n" +
                "has_target(demo, service473016340).\n" +
                "service(service473016340).\n" +
                "has_endpoint(service473016340,\"(ahc|ahc-ws|cxf|cxfbean|cxfrs)://.*\").\n" +
                "receives_label(demo) :- label(purpose(green)).\n" // Note that Prolog does not
                // support
                // nested predicates.
                +
                "has_decision(demo, allow).\n" +
                "\n" +
                "%%%%% Services %%%%%%%%%%%%\n" +
                "service(serviceAll).\n" +
                "has_endpoint(serviceAll,'.*').\n" +
                "creates_label(serviceAll, purpose(green)).\n" +
                "\n" +
                "service(sanitizedata).\n" +
                "has_endpoint(sanitizedata, \"^bean://SanitizerBean.*\").\n" +
                "creates_label(sanitizedata, public).\n" +
                "removes_label(sanitizedata, private).\n")

        // Route from LUCON paper with path searching logic
        private const val VERIFIABLE_ROUTE =
            ("%\n" +
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
                "\n")
    }
}
