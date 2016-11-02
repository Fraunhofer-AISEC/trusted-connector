package de.fhg.aisec.ids.cm.impl.docker;

import static org.junit.Assert.*;


import java.util.List;
import java.util.Optional;
import org.hamcrest.CoreMatchers.*;
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
		List<ApplicationContainer> lRunning = d.list(true);
		assertNotNull(lRunning);
		List<ApplicationContainer> lAll = d.list(false);
		assertNotNull(lAll);

		// we cannot have less running than total containers
		assertTrue(lAll.size() >= lRunning.size());
	}

	@Test
	public void testPull() {
		DockerCM d = new DockerCM();
		
		// Pull the smallest possible image. Blocks. (must be online)
		Optional<String> oContainerID = d.pullImage("true");
		
		// We expect a new container to be created 
		assertTrue(oContainerID.isPresent());
		assertNotEquals("", oContainerID.get());
		
		// we expect the container to be in list()
		List<ApplicationContainer> containers = d.list(false);
		Optional<ApplicationContainer> container = containers.stream().filter(c -> c.getId().equals(oContainerID.get())).findAny();
		assertTrue(container.isPresent());
		
		assertEquals(container.get().getStatus(), "CREATED");
	}

	@Test
	public void testStartStop() {
		DockerCM d = new DockerCM();
		
		// Pull the smallest possible image. Blocks. (must be online)
		Optional<String> oContainerID = d.pullImage("true");
		
		// We expect a new container to be created 
		assertTrue(oContainerID.isPresent());
		String containerID = oContainerID.get();
		
		// we expect the container to be in list()
		List<ApplicationContainer> containers = d.list(false);
		Optional<ApplicationContainer> container = containers.stream().filter(c -> c.getId().equals(oContainerID.get())).findAny();
		assertTrue(container.isPresent());
		
		assertEquals(container.get().getStatus(), "CREATED");
		
		d.startContainer(containerID);

		assertEquals(container.get().getStatus(), "RUNNING");
		
		d.stopContainer(containerID);
		
		assertEquals(container.get().getStatus(), "STOPPED");
	}
}
