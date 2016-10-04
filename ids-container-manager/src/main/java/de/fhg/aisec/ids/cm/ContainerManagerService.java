package de.fhg.aisec.ids.cm;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;
import de.fhg.aisec.ids.cm.impl.docker.DockerCM;
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM;

/**
 * Main entry point of the Container Management Layer.
 * 
 * This class is mainly a facade for the actual CML implementation, which can either be Docker or trust-X.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(enabled=true, immediate=true, name="ids-cml")
public class ContainerManagerService implements ContainerManager {
	private final static Logger LOG = LoggerFactory.getLogger(ContainerManagerService.class);
	private ContainerManager containerManager = null;

	@Activate
	protected void activate() {
		System.out.println("Activating Container Manager");
		// When activated, try to set container management instance
		Optional<ContainerManager> cm = getDefaultCM();
		if (cm.isPresent()) {
			LOG.info("Default container management is " + cm.get());
			containerManager = cm.get();
			
			List<ApplicationContainer> conts = containerManager.list(false);
			for (ApplicationContainer cont:conts) {
				System.out.println("   Container: " + cont);
			}
		} else {
			LOG.info("There is no supported container management");
		}
		
	}
	
	@Deactivate
	protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
		
	}
	
	
	private Optional<ContainerManager> getDefaultCM() {
		Optional<ContainerManager> result = Optional.<ContainerManager>empty();
		if (TrustXCM.isSupported()) {
			result = Optional.of(new TrustXCM());
		} else if (DockerCM.isSupported()) {
			result = Optional.of(new DockerCM());
		} else {
			LOG.warn("No supported container management layer found");
		}
		return result;
	}

	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		return containerManager.list(onlyRunning);
	}

	@Override
	public void wipe(String containerID) {
		containerManager.wipe(containerID);
	}

	@Override
	public void startContainer(String containerID) {
		containerManager.startContainer(containerID);
	}

	@Override
	public void stopContainer(String containerID) {
		containerManager.stopContainer(containerID);
	}

	@Override
	public void restartContainer(String containerID) {
		containerManager.restartContainer(containerID);
	}

	@Override
	public void pullImage(String imageID) {
		containerManager.pullImage(imageID);
	}

	@Override
	public String inspectContainer(String containerID) {
		return containerManager.inspectContainer(containerID);
	}

	@Override
	public Object getMetadata(String containerID) {
		return containerManager.getMetadata(containerID);
	}

	@Override
	public void setIpRule(String containerID, Direction direction, int srcPort, int dstPort, String srcDstRange,
			Protocol protocol, Decision decision) {
		containerManager.setIpRule(containerID, direction, srcPort, dstPort, srcDstRange, protocol, decision);
	}
}
