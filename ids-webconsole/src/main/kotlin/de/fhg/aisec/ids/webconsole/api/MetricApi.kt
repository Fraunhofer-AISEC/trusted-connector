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
package de.fhg.aisec.ids.webconsole.api

import de.fhg.aisec.ids.webconsole.ApiController
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import javax.ws.rs.core.MediaType

/**
 * REST API interface for platform metrics.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/metric/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
</method> */

@ApiController
@RequestMapping("/metric")
@Api(value = "Runtime Metrics", authorizations = [Authorization(value = "oauth2")])
class MetricApi {
    /**
     * Returns map of metrics.
     *
     * @return Map with system metrics
     */
    @GetMapping("get", produces = [MediaType.APPLICATION_JSON])
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "Map of metrics values",
            response = String::class,
            responseContainer = "Map"
        )
    )
    @ApiOperation(value = "Returns metrics values")
    fun getMetrics(): Map<String, String> {
        val result = HashMap<String, String>()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val mem = ManagementFactory.getMemoryMXBean()
        val threads = ManagementFactory.getThreadMXBean()
        val cl = ManagementFactory.getClassLoadingMXBean()
        val loadAvg = os.systemLoadAverage
        result["cpu.availableprocessors"] = Runtime.getRuntime().availableProcessors().toString()
        result["cpu.loadavg"] = if (loadAvg >= 0) loadAvgFormat.format(loadAvg) else "N/A"
        result["mem.free"] = Runtime.getRuntime().freeMemory().toString()
        result["mem.max"] = Runtime.getRuntime().maxMemory().toString()
        result["mem.total"] = Runtime.getRuntime().totalMemory().toString()
        result["mem.heapusage"] = mem.heapMemoryUsage.toString()
        result["mem.nonheapusage"] = mem.nonHeapMemoryUsage.toString()
        result["mem.objfinalizationcount"] = mem.objectPendingFinalizationCount.toString()
        result["thread.peakcount"] = threads.peakThreadCount.toString()
        result["thread.count"] = threads.threadCount.toString()
        result["thread.totalstartedcount"] = threads.totalStartedThreadCount.toString()
        result["cl.currentcount"] = cl.loadedClassCount.toString()
        result["cl.totalcount"] = cl.totalLoadedClassCount.toString()
        result["cl.unloadedcount"] = cl.unloadedClassCount.toString()
        result["os.arch"] = os.arch
        result["os.version"] = os.version
        result["os.name"] = os.name.toString()
        return result
    }

    companion object {
        private val loadAvgFormat = DecimalFormat("###.##")
    }
}
