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
package de.fhg.aisec.ids.api.router

/**
 * Bean representing a "route" (e.g., an Apache Camel route)
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
class RouteObject {
    var status: String? = null
    var uptime: Long = 0
    var context: String? = null
    var shortName: String? = null
    var dot: String? = null
    var description: String? = null
    var id: String? = null

    constructor() {
        // Bean std c'tor
    }

    constructor(
        id: String?,
        description: String?,
        dot: String?,
        shortName: String?,
        context: String?,
        uptime: Long,
        status: String?
    ) {
        this.id = id
        this.description = description
        this.dot = dot
        this.shortName = shortName
        this.context = context
        this.uptime = uptime
        this.status = status
    }
}
