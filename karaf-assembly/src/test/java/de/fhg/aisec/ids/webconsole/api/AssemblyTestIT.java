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
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

import java.io.File;
import java.net.MalformedURLException;

import javax.inject.Inject;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
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
//import org.apache.karaf.shell.api.console.Session;
//import org.apache.karaf.shell.api.console.SessionFactory;
import org.osgi.service.cm.ConfigurationAdmin;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class AssemblyTestIT {

    @Inject
    protected BundleContext bundleContext;
    @Inject
    protected FeaturesService featuresService;
    @Inject
    protected BootFinished bootFinished;
	
	@Inject
	@Filter(timeout = 30000)	
	private ConnectionManager conm;

	@Inject
	@Filter(timeout = 30000)	
	private AcmeClient acme;

	@Inject
	@Filter(timeout = 30000)	
	private Settings settings;

	@Inject
	@Filter(timeout = 30000)	
	private ConfigurationAdmin configAdmin;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
}
	@Configuration
	public Option[] config() throws MalformedURLException {
		MavenUrlReference camelRepo = maven()
				.groupId("org.apache.camel.karaf")
				.artifactId("apache-camel")
				.classifier("features")
				.type("xml")
				.version("2.21.2");
		MavenUrlReference idsRepo = maven()
				.groupId("de.fhg.ids")
				.artifactId("karaf-features-ids")
				.classifier("features")
				.type("xml")
				.version("1.2.0-SNAPSHOT");
		File f = new File("./build/karaf-assembly-1.2.0-SNAPSHOT.tar.gz");
		return new Option[] { karafDistributionConfiguration().frameworkUrl(f.toURI().toURL().toString())
				.unpackDirectory(new File("build/exam"))
				.useDeployFolder(false),
				keepRuntimeFolder(),
				features(idsRepo, "ids"),
				junitBundles() };
	}

	@Test
	public void testSettings() throws Exception {
		ConnectorConfig cc = settings.getConnectorConfig();
		assertNotNull(cc);
		assertNotNull(cc.getAppstoreUrl());
		assertNotNull(cc.getAcmeDnsWebcon());
		assertNotNull(cc.getAcmePortWebcon());
		assertNotNull(cc.getAcmeServerWebcon());
		assertNotNull(cc.getAppstoreUrl());
		assertNotNull(cc.getBrokerUrl());
		assertNotNull(cc.getTtpHost());
	}

	@Test
	public void testConfigAdmin() throws Exception {
		org.osgi.service.cm.Configuration conf = configAdmin.getConfiguration("ids");
		assertNotNull(conf);
	}

	@Test
	public void testConnectionManager() throws Exception {
		assertNotNull(conm.listAvailableEndpoints());
		assertNotNull(conm.listIncomingConnections());
		assertNotNull(conm.listOutgoingConnections());
	}
}
