package de.fhg.aisec.ids.cm.impl.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;

public class DockerCmIT {
	private List<ApplicationContainer> wipes = new ArrayList<ApplicationContainer>();
	
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
	
	@After
	public void cleanUp() {
		// Remove containers created during test
		DockerCM d = new DockerCM();
		wipes.forEach(c -> d.wipe(c.getNames()));
	}

	@Test
	public void testList() {
		DockerCM d = new DockerCM();
		
		// List running containers
		List<ApplicationContainer> lRunning = d.list(true);
		assertNotNull(lRunning);
		
		// List all containers (also stopped ones)
		List<ApplicationContainer> lAll = d.list(false);
		assertNotNull(lAll);

		// we cannot have less running than total containers
		assertTrue(lAll.size() >= lRunning.size());
	}

	@Test
	public void testPull() {
		DockerCM d = new DockerCM();
		
		// Pull the smallest possible image. Blocks. (must be online)
		Optional<String> oContainerID = d.pullImage("tianon/true");
		
		// We expect a new container to be created 
		assertTrue(oContainerID.isPresent());
		assertNotEquals("", oContainerID.get());
		
		// we expect the container to be in list()
		List<ApplicationContainer> containers = d.list(false);
		Optional<ApplicationContainer> container = containers.stream().filter(c -> c.getNames().equals(oContainerID.get())).findAny();
		assertTrue(container.isPresent());
		wipes.add(container.get());
		
		assertEquals("Created", container.get().getStatus());
	}

	@Test
	public void testStartStop() {
		DockerCM d = new DockerCM();
		
		// Pull an image we can actually start. (must be online)
		Optional<String> oContainerID = d.pullImage("nginx");
		
		// We expect a new container to be created 
		assertTrue(oContainerID.isPresent());
		String containerID = oContainerID.get();
		assertNotNull(containerID);
		
		// we expect the container to be in list()
		List<ApplicationContainer> containers = d.list(false);
		containers.forEach(x -> System.out.println(x.getId()));
		Optional<ApplicationContainer> container = containers.stream().filter(c -> c.getNames().equals(oContainerID.get())).findAny();
		assertTrue(container.isPresent());
		wipes.add(container.get());
		
		assertEquals("Created", container.get().getStatus());
		
		// Start container
		d.startContainer(containerID);

		// We now expect it in list of running containers
		containers = d.list(true);		
		Optional<ApplicationContainer> runningContainer = containers.stream().filter(c -> c.getNames().equals(containerID)).findAny();
		assertTrue(runningContainer.isPresent());
		assertTrue(runningContainer.get().getStatus().startsWith("Up"));
		
		// Stop container
		d.stopContainer(containerID);

		// We expect it to be still in list of all containers
		containers = d.list(false);		
		Optional<ApplicationContainer> stoppedContainer = containers.stream().filter(c -> c.getNames().equals(containerID)).findAny();
		assertTrue(stoppedContainer.isPresent());
		assertTrue(stoppedContainer.get().getStatus().startsWith("Exited"));
	}
}
