/*-
 * ========================LICENSE_START=================================
 * ids-api
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
package de.fhg.aisec.ids.api.infomodel

import de.fraunhofer.iais.eis.SecurityProfile
import de.fraunhofer.iais.eis.util.TypedLiteral
import java.io.Serializable
import java.net.URI

class ConnectorProfile : Serializable {
    var securityProfile: SecurityProfile?
    var connectorUrl: URI?
    var maintainerUrl: URI?
    var connectorEntityNames: List<TypedLiteral>?

    constructor() {
        securityProfile = SecurityProfile.TRUST_SECURITY_PROFILE
        connectorUrl = null
        maintainerUrl = null
        connectorEntityNames = null
    }

    constructor(
        profile: SecurityProfile?,
        connectorUrl: URI?,
        maintainerUrl: URI?,
        connectorEntityNames: List<TypedLiteral>?
    ) : super() {
        securityProfile = profile
        this.connectorUrl = connectorUrl
        this.maintainerUrl = maintainerUrl
        this.connectorEntityNames = connectorEntityNames
    }

    companion object {
        private const val serialVersionUID = 2L
    }
}
