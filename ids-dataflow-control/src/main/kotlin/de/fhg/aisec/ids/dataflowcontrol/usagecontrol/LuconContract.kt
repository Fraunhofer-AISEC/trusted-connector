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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.Collections

class LuconContract private constructor(contract: ContractAgreement) {
    val permissions = contract.permission.map(::LuconPermission)
    private val contractId: String = contract.id.toString()

    fun enforce(ectx: EnforcementContext) {
        val enforcementErrors = linkedSetOf<String>()
        val enforcementSuccessful = permissions.withIndex().any { (i, p) ->
            try {
                if (ectx.log.isDebugEnabled) {
                    ectx.log.debug("Checking permission # ${i + 1} of contract $contractId...")
                }
                p.checkEnforcible(ectx)
                if (!ectx.ucPolicies.isEmpty) {
                    LOG.warn(
                        "UC policies have been added to EnforcementContext in LuconPermission::checkEnforcible(), " +
                            "before actual enforcement in LuconPermission::enforce()." +
                            "This is STRICTLY DISCOURAGED, as it can result in incorrect UC policies!"
                    )
                }
                p.enforce(ectx)
                true
            } catch (t: Throwable) {
                if (t !is LuconException) {
                    LOG.error("Unexpected UC exception", t)
                }
                enforcementErrors += t.message.toString()
                ectx.log.let { log ->
                    if (log.isDebugEnabled) {
                        log.debug(t.message)
                    }
                }
                false
            }
        }
        if (enforcementSuccessful) {
            val client = OkHttpClient.Builder().build()
            val ucUrl = "http://${ectx.endpointUri.host}/usage-control"
            val request = Request.Builder()
                .url(ucUrl)
                .post(ectx.ucPolicies.toString().toRequestBody(MEDIA_TYPE_JSON))
                .build()
            if (!client.newCall(request).execute().isSuccessful) {
                throw LuconException("Enforcement checks were successful, but POSTing UC policies to $ucUrl failed.")
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
        val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        private val contracts: MutableMap<URI, LuconContract> = Collections.synchronizedMap(hashMapOf<URI, LuconContract>())
        fun getContract(contract: ContractAgreement) =
            contracts.computeIfAbsent(contract.id) {
                LuconContract(contract)
            }
    }
}
