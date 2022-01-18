/*-
 * ========================LICENSE_START=================================
 * camel-multipart-processor
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
package de.fhg.aisec.ids.camel.multipart

import de.fhg.aisec.ids.api.infomodel.InfoModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * The only purpose of this OSGi component is to connect to the InfoModelManager.
 *
 * This is required for the MultiPartComponent to use a proper IDS self description in the multipart
 * messages.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
@Component("idsMultipartComponent")
class MultiPartComponent {
    @Autowired
    lateinit var infoModelManager: InfoModel

    init {
        instance = this
    }

    companion object {
        private lateinit var instance: MultiPartComponent

        val infoModelManager
            get() = instance.infoModelManager
    }
}
