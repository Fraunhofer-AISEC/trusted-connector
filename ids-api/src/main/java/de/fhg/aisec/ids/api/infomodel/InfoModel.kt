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

import de.fraunhofer.iais.eis.Connector

interface InfoModel {
    /**
     * Retrieve Connector object based on stored properties
     *
     * @return currently stored Connector object
     */
    val connector: Connector?

    /**
     * Save/Update Connector object to preferences
     *
     * @param profile Basic connector profile from GUI
     * @return update success
     */
    fun setConnector(profile: ConnectorProfile): Boolean

    /**
     * Get connector self-description as JSON-LD
     *
     * @return Connector self-description as JSON-LD
     */
    val connectorAsJsonLd: String

    /**
     * Set static connector self-description as JSON-LD, or remove if "null" is passed
     *
     * @param jsonLd JSON-LD self-description to use for this connector
     */
    fun setConnectorByJsonLd(jsonLd: String?)

    /**
     * Get connector Dynamic Attribute Token
     *
     * @return DAT
     */
    val dynamicAttributeToken: String

    /**
     * Set Dynamic Attribute Token or remove if "null" is passed
     *
     * @param dynamicAttributeToken The DAT to use for this connector
     * @return Whether storing the DAT has been successful
     */
    fun setDynamicAttributeToken(dynamicAttributeToken: String?)

    /**
     * Get version of implemented infomodel
     *
     * @return Version of infomodel
     */
    val modelVersion: String

    /**
     * Initializes an infomodel *MessageBuilder with required fields,
     * using reflection
     *
     * @return Populated *MessageBuilder
     */
    fun <T: Any> initMessageBuilder(builder: T): T
}