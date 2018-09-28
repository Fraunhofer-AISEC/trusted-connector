/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Test;

public class PortainerCompatibilityIT {

  /**
   * Retrieves portainer templates as JSON and tries to map it to java using Jackson objectmapper.
   *
   * @throws IOException
   */
  @Test
  public void test() throws IOException {
    String url = "https://raw.githubusercontent.com/portainer/templates/master/templates.json";
    URL u = new URL(url);
    HttpURLConnection c = (HttpURLConnection) u.openConnection();
    c.setRequestMethod("GET");
    c.setRequestProperty("Content-length", "0");
    c.setUseCaches(false);
    c.setAllowUserInteraction(false);
    c.setConnectTimeout(3000);
    c.setReadTimeout(3000);
    c.connect();
    int status = c.getResponseCode();
    String json = "";
    switch (status) {
      case 200:
      case 201:
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        br.close();
        json = sb.toString();
    }
    ObjectMapper mapper = new ObjectMapper();
    ApplicationContainer[] cont = mapper.readValue(json.getBytes(), ApplicationContainer[].class);

    assertNotNull(cont);
    assertTrue(cont.length > 0);
  }
}
