package de.fhg.ids.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

import de.fhg.ids.comm.ws.protocol.metadata.MetadataHandler;

public class Docker {

	private DockerClient dockerClient;
	private List<String> containerIDs = new ArrayList<String>();
	private List<InspectContainerResponse> containerResponses = new ArrayList<InspectContainerResponse>();
	private Logger LOG = LoggerFactory.getLogger(Docker.class);
	
	public void connectClient() {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
		  .withDockerHost("unix:///var/run/docker.sock")
		  .build();

		this.dockerClient = DockerClientBuilder.getInstance(config)
		  .build();

	}
	
	public List<InspectContainerResponse> getMetaData() {
		for(Container container: dockerClient.listContainersCmd().exec()){
			LOG.debug(container.getId());
	    	containerIDs.add(container.getId());
	    	containerResponses.add(dockerClient.inspectContainerCmd(container.getId()).exec());
	    }
		
		return containerResponses;
	}
}
