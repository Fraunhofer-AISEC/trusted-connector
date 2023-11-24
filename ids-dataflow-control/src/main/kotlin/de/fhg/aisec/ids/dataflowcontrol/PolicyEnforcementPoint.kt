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

import com.google.common.collect.MapMaker
import de.fhg.aisec.ids.api.policy.DecisionRequest
import de.fhg.aisec.ids.api.policy.PolicyDecision
import de.fhg.aisec.ids.api.policy.ServiceNode
import de.fhg.aisec.ids.api.policy.TransformationDecision
import de.fhg.aisec.ids.dataflowcontrol.usagecontrol.EnforcementContext
import de.fhg.aisec.ids.dataflowcontrol.usagecontrol.LuconContract
import de.fhg.aisec.ids.dataflowcontrol.usagecontrol.LuconException
import org.apache.camel.AsyncCallback
import org.apache.camel.Exchange
import org.apache.camel.NamedNode
import org.apache.camel.Processor
import org.apache.camel.model.EndpointRequiredDefinition
import org.apache.camel.model.RouteDefinition
import org.apache.camel.model.ToDefinition
import org.apache.camel.support.processor.DelegateAsyncProcessor
import org.slf4j.LoggerFactory
import java.net.URI

class PolicyEnforcementPoint
    internal constructor(private val destinationNode: NamedNode, target: Processor) :
    DelegateAsyncProcessor(target) {
        /**
         * The method performs flow control and calls Exchange.setException() when necessary It iterates
         * through nodes in CamelRoute (<from>, <log>, <choice>, <process>, <to>, ...) and launches node
         * specific usage enforcement actions.
         *
         * At node <from>: protect exchange msg's body if usage constraint is given At node <to>:
         * unprotect exchange msg's body if usage constraing it fulfilled
         *
         * @param exchange The exchange object to check
         * @return Whether target.process() is to be called for this exchange object
         */
        private fun processFlowControl(exchange: Exchange?): Boolean {
            if (exchange == null) {
                if (LOG.isWarnEnabled) {
                    LOG.warn("Cannot check data flow policy. Exchange object is null.")
                }
                return false
            }
            // Strict policy: If PDP or Usage Control interface are not available, block every checked
            // data flow
            val pdp = CamelInterceptor.pdp ?: throw Exception("PDP is not available")
            val ucInterface =
                CamelInterceptor.usageControlInterface
                    ?: throw Exception("Usage Control Interface is not available")

            // Save per exchange object the source node's content (<from: uri="idscp2server://0...>)
            // Same entry in Hashmap per CamelRoute, change of exchange properties do not create new
            // entry
            val sourceNode: NamedNode =
                lastDestinations[exchange]
                    ?: run {
                        // If there is no previously saved node for this exchange, use the parent
                        // <route> to find the input
                        // (first <from> statement) of the processed route.
                        var routeNode = destinationNode.parent
                        while (routeNode !is RouteDefinition) {
                            routeNode = routeNode.parent
                        }
                        routeNode.input
                    }

            // Save current destination node of CamelRoute
            lastDestinations[exchange] = destinationNode
            val source = sourceNode.toString()
            val destination = destinationNode.toString()
            if (LOG.isTraceEnabled) {
                LOG.trace("{} -> {}", source, destination)
            }

            // Only take action if previous node was a <from> node (input)
            // or if current node or previous node is or has been a <to> node (output and probably also input)
            if (sourceNode is EndpointRequiredDefinition || destinationNode is ToDefinition) {
                // If there is no known ContractAgreement for this Exchange, nothing to do here.
                ucInterface.getExchangeContract(exchange)?.let { contract ->
                    val luconContract = LuconContract.getContract(contract)
                    // Check whether body protection has not yet been performed.
                    if (!ucInterface.isProtected(exchange)) {
                        ucInterface.protectBody(exchange, contract.id)
                        LOG.debug("UC: Protect Exchange body with UC contract {}", contract.id)
                    }
                    // Check whether we are about to send data via a <to> node
                    if (destinationNode is ToDefinition) {
                        val endpointUri = URI.create(destinationNode.endpointUri)
                        val enforcementContext = EnforcementContext(endpointUri, LOG)
                        try {
                            luconContract.enforce(enforcementContext)
                            // Restore exchange body
                            ucInterface.unprotectBody(exchange)
                            LOG.debug("UC: Contract permits data flow, Exchange body has been restored.")
                        } catch (le: LuconException) {
                            LOG.warn(le.message)
                        }
                    }
                }
            }

            val sourceServiceNode = ServiceNode(source)
            val destinationServiceNode = ServiceNode(destination)

            // Call PDP to transform labels and decide whether to forward the Exchange
            applyLabelTransformation(pdp.requestTranformations(sourceServiceNode), exchange)
            val labels = exchangeLabels.computeIfAbsent(exchange) { HashSet() }
            val decision =
                pdp.requestDecision(
                    DecisionRequest(sourceServiceNode, destinationServiceNode, labels, null)
                )
            return when (decision.decision) {
                PolicyDecision.Decision.ALLOW -> true
                PolicyDecision.Decision.DENY -> {
                    if (LOG.isWarnEnabled) {
                        LOG.warn(
                            "Exchange explicitly blocked (DENY) by data flow policy. " +
                                "Source: {}, Target: {}",
                            sourceServiceNode,
                            destinationServiceNode
                        )
                    }
                    exchange.setException(Exception("Exchange blocked by data flow policy"))
                    false
                }
            }
        }

        /**
         * Removes and adds labels to an exchange object.
         *
         * @param requestTransformations The request transformations (label changes) to be performed
         * @param exchange Exchange processed
         */
        private fun applyLabelTransformation(
            requestTransformations: TransformationDecision,
            exchange: Exchange
        ) {
            val labels = exchangeLabels.computeIfAbsent(exchange) { HashSet() }

            // Remove labels from exchange
            labels.removeAll(requestTransformations.labelsToRemove)

            // Add labels to exchange
            labels.addAll(requestTransformations.labelsToAdd)
        }

        @Throws(Exception::class)
        override fun process(
            exchange: Exchange,
            asyncCallback: AsyncCallback
        ): Boolean {
            if (processFlowControl(exchange)) {
                return super.process(exchange, asyncCallback)
            } else {
                throw Exception("Exchange blocked by data flow policy")
            }
        }

        companion object {
            private val LOG = LoggerFactory.getLogger(PolicyEnforcementPoint::class.java)
            private val lastDestinations: MutableMap<Exchange, NamedNode> =
                MapMaker().weakKeys().makeMap()
            private val exchangeLabels: MutableMap<Exchange, MutableSet<String>> =
                MapMaker().weakKeys().makeMap()
        }
    }
