package de.fhg.aisec.ids.cm.impl.docker;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;

public class DockerCMTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		// Skip all tests if docker is not supported on the current platform
		if (!DockerCM.isSupported()) {
			Assume.assumeTrue(true);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testList() {
		DockerCM d = new DockerCM();
		List<ApplicationContainer> l = d.list(false);
		assertNotNull(l);
		//TODO implement proper tests
	}
}
