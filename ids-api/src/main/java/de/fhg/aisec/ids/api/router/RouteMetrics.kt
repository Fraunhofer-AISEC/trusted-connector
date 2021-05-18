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
 * Metrics of a single route.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
class RouteMetrics {
    var completed: Long = 0
    var failed: Long = 0
    var inflight: Long = 0
    var failuresHandled: Long = 0
    var redeliveries: Long = 0
    var maxProcessingTime: Long = 0
    var meanProcessingTime: Long = 0
    var minProcessingTime: Long = 0
}
