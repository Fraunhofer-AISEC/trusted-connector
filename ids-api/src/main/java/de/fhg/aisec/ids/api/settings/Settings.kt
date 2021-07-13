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
package de.fhg.aisec.ids.api.settings

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile

interface Settings {
    var connectorConfig: ConnectorConfig
    var connectorProfile: ConnectorProfile
    var connectorJsonLd: String?
    fun getConnectionSettings(connection: String): ConnectionSettings
    fun setConnectionSettings(connection: String, cSettings: ConnectionSettings)
    val allConnectionSettings: Map<String, ConnectionSettings>
    fun isUserStoreEmpty(): Boolean
    fun getUserHash(username: String): String?
    fun saveUser(username: String, hash: String)
    fun removeUser(username: String)
    fun setPassword(username: String, hash: String)
    fun getUsers(): Map<String, String>
}
