/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
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
package de.fhg.aisec.ids.dataflowcontrol.usagecontrol

import de.fraunhofer.iais.eis.ContractAgreement
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Collections

class LuconContract private constructor(contract: ContractAgreement) {
    val permissions = contract.permission.map(::LuconPermission)
    private val contractId: String = contract.id.toString()

    fun enforce(eCtx: EnforcementContext) {
        val enforcementErrors = linkedSetOf<String>()
        val enforcementSuccessful = permissions.withIndex().any { (i, p) ->
            try {
                if (eCtx.log.isDebugEnabled) {
                    eCtx.log.debug("Checking permission # ${i + 1} of contract $contractId...")
                }
                p.checkEnforcible(eCtx)
                if (eCtx.ucPolicies.isNotEmpty()) {
                    LOG.warn(
                        "UC policies have been added to EnforcementContext in LuconPermission::checkEnforcible(), " +
                            "before actual enforcement in LuconPermission::enforce()." +
                            "This is STRICTLY DISCOURAGED, as it can result in incorrect UC policies!"
                    )
                }
                p.enforce(eCtx)
                true
            } catch (t: Throwable) {
                if (t !is LuconException) {
                    LOG.error("Unexpected UC exception", t)
                }
                enforcementErrors += t.message.toString()
                eCtx.log.let { log ->
                    if (log.isDebugEnabled) {
                        log.debug(t.message)
                    }
                }
                false
            }
        }
        if (enforcementSuccessful) {
            runBlocking(Dispatchers.IO) {
                val ucUrl = "http://${eCtx.endpointUri.host}/usage-control"
                val response = HTTP_CLIENT.post(ucUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(eCtx.ucPolicies)
                }
                if (response.status.value !in 200..299) {
                    throw LuconException(
                        "Enforcement checks were successful, but POSTing UC policies to $ucUrl failed."
                    )
                }
            }
        } else {
            throw LuconException(
                "UC: Policy enforcement failed, i.e. all permissions failed:\n" +
                    enforcementErrors.joinToString("\n")
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LuconContract::class.java)
        private val HTTP_CLIENT = HttpClient(Java) {
            install(ContentNegotiation) {
                jackson()
            }
        }
        private val contracts: MutableMap<URI, LuconContract> = Collections.synchronizedMap(hashMapOf<URI, LuconContract>())
        fun getContract(contract: ContractAgreement) =
            contracts.computeIfAbsent(contract.id) {
                LuconContract(contract)
            }
    }
}
