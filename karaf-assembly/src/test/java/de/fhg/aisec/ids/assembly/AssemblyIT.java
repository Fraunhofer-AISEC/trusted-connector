/*-
 * ========================LICENSE_START=================================
 * karaf-assembly
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
package de.fhg.aisec.ids.assembly;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionBaseConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AssemblyIT extends KarafTestSupport {

  @Inject
  @Filter(timeout = 30000)
  private ConnectionManager connectionManager;

  @Inject
  @Filter(timeout = 30000)
  private Settings settings;

  @Override
  public MavenArtifactUrlReference getKarafDistribution() {
    return new MavenArtifactUrlReference() {
      @Override
      public String getURL() {
        try {
          return new File("build/karaf-assembly-" + System.getProperty("project.version") + ".tar.gz")
              .toURI().toURL().toString();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Configuration
  public Option[] config() {
    final var options = new LinkedList<>(Arrays.asList(super.config()));
    // Modify distribution config
    ((KarafDistributionBaseConfigurationOption) options.get(0))
        .unpackDirectory(new File("build/exam"))
        .useDeployFolder(false);
    // Fix log level
    options.replaceAll(o -> {
      if (o instanceof LogLevelOption) {
        return KarafDistributionOption.logLevel(LogLevelOption.LogLevel.TRACE);
      } else {
        return o;
      }
    });
    return options.toArray(new Option[0]);
  }

  @Test
  public void testSettings() {
    ConnectorConfig cc = settings.getConnectorConfig();
    assertNotNull(cc);
    assertNotNull(cc.getAppstoreUrl());
    assertNotNull(cc.getAcmeDnsWebcon());
    assertNotEquals(0, cc.getAcmePortWebcon());
    assertNotNull(cc.getAcmeServerWebcon());
    assertNotNull(cc.getAppstoreUrl());
    assertNotNull(cc.getBrokerUrl());
    assertNotNull(cc.getTtpHost());
  }

  @Test
  public void testConnectionManager() {
    assertNotNull(connectionManager.listAvailableEndpoints());
    assertNotNull(connectionManager.listIncomingConnections());
    assertNotNull(connectionManager.listOutgoingConnections());
  }
}
