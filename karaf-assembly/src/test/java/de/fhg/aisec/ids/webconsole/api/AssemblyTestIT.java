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
package de.fhg.aisec.ids.webconsole.api;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@Ignore("Broken with Java 11, Pax Exam complains that container doesn't come up although it has been tested OK.")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class AssemblyTestIT {

  @Inject protected BundleContext bundleContext;
  @Inject protected FeaturesService featuresService;
  @Inject protected BootFinished bootFinished;

  @Inject
  @Filter(timeout = 30000)
  private ConnectionManager conm;

  @Inject
  @Filter(timeout = 30000)
  private AcmeClient acme;

  @Inject
  @Filter(timeout = 30000)
  private Settings settings;

  @ProbeBuilder
  public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
    probe.setHeader(
        Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
    return probe;
  }

  @Configuration
  public Option[] config() throws MalformedURLException {
    MavenUrlReference camelRepo =
        maven()
            .groupId("org.apache.camel.karaf")
            .artifactId("apache-camel")
            .classifier("features")
            .type("xml")
            .version("2.21.2");
    MavenUrlReference idsRepo =
        maven()
            .groupId("de.fhg.aisec.ids")
            .artifactId("karaf-features-ids")
            .classifier("features")
            .type("xml")
            .version(System.getProperty("project.version"));
    File f = new File("build/karaf-assembly-" + System.getProperty("project.version") + ".tar.gz");
    return new Option[] {
      karafDistributionConfiguration()
          .frameworkUrl(f.toURI().toURL().toString())
          .unpackDirectory(new File("build/exam"))
          .useDeployFolder(false),
      keepRuntimeFolder(),
      features(idsRepo, "ids"),
      junitBundles()
    };
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
    assertNotNull(conm.listAvailableEndpoints());
    assertNotNull(conm.listIncomingConnections());
    assertNotNull(conm.listOutgoingConnections());
  }
}
