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
package de.fhg.aisec.ids.acme.provider;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.shredzone.acme4j.provider.AbstractAcmeProvider;
import org.shredzone.acme4j.provider.AcmeProvider;

/**
 * An {@link AcmeProvider} for <em>Boulder</em>.
 *
 * @see <a href="https://github.com/letsencrypt/boulder">Boulder</a>
 */
public class BoulderAcmeProvider extends AbstractAcmeProvider {

  private static final Pattern HOST_PATTERN = Pattern.compile("^/([^:/]+)(?::(\\d+))?/?$");
  public static final int DEFAULT_PORT = 4001;

  @Override
  public boolean accepts(URI serverUri) {
    return "acme".equals(serverUri.getScheme()) && "boulder".equals(serverUri.getHost());
  }

  @Override
  public URL resolve(URI serverUri) {
    try {
      String path = serverUri.getPath();

      URL baseUrl = new URL("http://localhost:" + DEFAULT_PORT + "/directory");

      if (path != null && !path.isEmpty() && !"/".equals(path)) {
        baseUrl = parsePath(path);
      }

      return baseUrl;
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Bad server URI " + serverUri, ex);
    }
  }

  /**
   * Parses the server URI path and returns the server's base URL.
   *
   * @param path server URI path
   * @return URL of the server's base
   */
  private URL parsePath(String path) throws MalformedURLException {
    Matcher m = HOST_PATTERN.matcher(path);
    if (m.matches()) {
      String host = m.group(1);
      int port = DEFAULT_PORT;
      if (m.group(2) != null) {
        port = Integer.parseInt(m.group(2));
      }
      return new URL("http", host, port, "/directory");
    } else {
      throw new IllegalArgumentException("Invalid Pebble host/port: " + path);
    }
  }
}
