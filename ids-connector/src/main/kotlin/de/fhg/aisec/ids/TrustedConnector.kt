/*-
 * ========================LICENSE_START=================================
 * ids-connector
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Main startup class for the Trusted Connector using Spring Boot. */
@SpringBootApplication
class TrustedConnector {

    companion object {

        val LOG = LoggerFactory.getLogger(TrustedConnector::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<TrustedConnector>(*args)
        }
    }
}
