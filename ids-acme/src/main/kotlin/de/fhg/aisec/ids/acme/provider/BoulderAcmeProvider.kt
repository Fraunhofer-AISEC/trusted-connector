/*-
 * ========================LICENSE_START=================================
 * ids-acme
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.acme.provider

import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.regex.Pattern
import org.shredzone.acme4j.provider.AbstractAcmeProvider
import org.shredzone.acme4j.provider.AcmeProvider

/**
 * An [AcmeProvider] for *Boulder*.
 *
 * @see [Boulder](https://github.com/letsencrypt/boulder)
 */
class BoulderAcmeProvider : AbstractAcmeProvider() {

    override fun accepts(serverUri: URI): Boolean {
        return "acme" == serverUri.scheme && "boulder" == serverUri.host
    }

    override fun resolve(serverUri: URI): URL {
        try {
            val path = serverUri.path

            var baseUrl = URL("http://localhost:$DEFAULT_PORT/directory")

            if (path != null && path != "" && "/" != path) {
                baseUrl = parsePath(path)
            }

            return baseUrl
        } catch (ex: MalformedURLException) {
            throw IllegalArgumentException("Bad server URI $serverUri", ex)
        }
    }

    /**
     * Parses the server URI path and returns the server's base URL.
     *
     * @param path server URI path
     * @return URL of the server's base
     */
    @Throws(MalformedURLException::class)
    private fun parsePath(path: String): URL {
        val m = HOST_PATTERN.matcher(path)
        if (m.matches()) {
            val host = m.group(1)
            var port = DEFAULT_PORT
            if (m.group(2) != null) {
                port = Integer.parseInt(m.group(2))
            }
            return URL("http", host, port, "/directory")
        } else {
            throw IllegalArgumentException("Invalid Pebble host/port: $path")
        }
    }

    companion object {

        private val HOST_PATTERN = Pattern.compile("^/([^:/]+)(?::(\\d+))?/?$")
        const val DEFAULT_PORT = 4001
    }
}
