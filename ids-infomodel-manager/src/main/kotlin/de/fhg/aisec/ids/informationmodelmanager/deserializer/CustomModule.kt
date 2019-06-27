/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.informationmodelmanager.deserializer

import com.fasterxml.jackson.databind.module.SimpleModule

import de.fraunhofer.iais.eis.SecurityProfile

// allows Jackson to deserialize custom object types
class CustomModule : SimpleModule("CustomModule") {
    init {
        this.addDeserializer(SecurityProfile::class.java, SecurityProfileDeserializer())
    }

    companion object {
        const val serialVersionUID = 1L
    }
}
