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
package de.fhg.aisec.ids.webconsole.api;

import io.swagger.annotations.*;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST API interface for platform metrics.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/metric/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Path("/metric")
@Api(
  value = "Runtime Metrics",
  authorizations = {@Authorization(value = "oauth2")}
)
public class MetricAPI {

  private static final DecimalFormat loadAvgFormat = new DecimalFormat("###.##");

  /**
   * Returns map of metrics.
   *
   * @return Map with system metrics
   */
  @GET
  @Path("get")
  @ApiOperation(value = "Returns metrics values")
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message = "Map of metrics values",
        response = String.class,
        responseContainer = "Map"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  @AuthorizationRequired
  public Map<String, String> getMetrics() {
    HashMap<String, String> result = new HashMap<>();

    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
    ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

    double loadAvg = os.getSystemLoadAverage();

    result.put(
        "cpu.availableprocessors", String.valueOf(Runtime.getRuntime().availableProcessors()));
    result.put("cpu.loadavg", loadAvg >= 0 ? loadAvgFormat.format(loadAvg) : "N/A");
    result.put("mem.free", String.valueOf(Runtime.getRuntime().freeMemory()));
    result.put("mem.max", String.valueOf(Runtime.getRuntime().maxMemory()));
    result.put("mem.total", String.valueOf(Runtime.getRuntime().totalMemory()));
    result.put("mem.heapusage", String.valueOf(mem.getHeapMemoryUsage()));
    result.put("mem.nonheapusage", String.valueOf(mem.getNonHeapMemoryUsage()));
    result.put("mem.objfinalizationcount", String.valueOf(mem.getObjectPendingFinalizationCount()));
    result.put("thread.peakcount", String.valueOf(threads.getPeakThreadCount()));
    result.put("thread.count", String.valueOf(threads.getThreadCount()));
    result.put("thread.totalstartedcount", String.valueOf(threads.getTotalStartedThreadCount()));
    result.put("cl.currentcount", String.valueOf(cl.getLoadedClassCount()));
    result.put("cl.totalcount", String.valueOf(cl.getTotalLoadedClassCount()));
    result.put("cl.unloadedcount", String.valueOf(cl.getUnloadedClassCount()));
    result.put("os.arch", os.getArch());
    result.put("os.version", os.getVersion());
    result.put("os.name", String.valueOf(os.getName()));

    return result;
  }
}
