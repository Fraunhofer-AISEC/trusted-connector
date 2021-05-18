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

import com.fasterxml.jackson.databind.ObjectMapper
import de.fhg.aisec.ids.api.cm.ApplicationContainer
import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class PortainerCompatibilityIT {
    /**
     * Retrieves portainer templates as JSON and tries to map it to java using Jackson objectmapper.
     *
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun test() {
        val url = "https://raw.githubusercontent.com/portainer/templates/master/templates.json"
        val u = URL(url)
        val c = u.openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.setRequestProperty("Content-length", "0")
        c.useCaches = false
        c.allowUserInteraction = false
        c.connectTimeout = 3000
        c.readTimeout = 3000
        c.connect()
        val status = c.responseCode
        var json = ""
        when (status) {
            200, 201 -> {
                val br = BufferedReader(InputStreamReader(c.inputStream))
                val sb = StringBuilder()
                var line: String
                while (br.readLine().also { line = it } != null) {
                    sb.append(
                        """
    $line
    
                        """.trimIndent()
                    )
                }
                br.close()
                json = sb.toString()
            }
        }
        val mapper = ObjectMapper()
        val cont = mapper.readValue(json.toByteArray(), Array<ApplicationContainer>::class.java)
        Assert.assertNotNull(cont)
        Assert.assertTrue(cont.isNotEmpty())
    }
}
