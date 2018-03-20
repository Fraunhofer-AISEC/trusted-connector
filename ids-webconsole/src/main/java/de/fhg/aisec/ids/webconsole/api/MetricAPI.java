/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.osgi.service.prefs.PreferencesService;

import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for platform metrics.
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/metric/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/metric")
public class MetricAPI {

	/**
	 * Returns map of metrics.
	 *
	 * @return Map with system metrics
	 */
	@GET
	@Path("get")
	@Produces("application/json")
	public Map<String, String> getMetrics() {
		HashMap<String, String> result = new HashMap<>();
		
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		ThreadMXBean threads = ManagementFactory.getThreadMXBean();
		ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

		double loadAvg = os.getSystemLoadAverage();
		
		result.put("cpu.availableprocessors", String.valueOf(Runtime.getRuntime().availableProcessors()));
		result.put("cpu.loadavg", loadAvg >= 0 ? String.valueOf(loadAvg) : "N/A");
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
