/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole.api.data

class Cert {
    var subjectC: String? = null
    var subjectS: String? = null
    var subjectL: String? = null
    var subjectO: String? = null
    var subjectOU: String? = null
    var subjectAltNames: Collection<List<*>>? = null
    var subjectCN: String? = null
    @kotlin.jvm.JvmField
    var alias: String? = null
    var file: String? = null
    var certificate: String? = null
}
