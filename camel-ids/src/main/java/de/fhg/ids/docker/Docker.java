/*
package de.fhg.ids.docker;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
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
	
	// initialize and fill arraylists by fetching data from unix socket
	public void connectClient() {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
		  .withDockerHost("unix:///var/run/docker.sock")
		  .build();

		this.dockerClient = DockerClientBuilder.getInstance(config)
		  .build();
		
		this.fetchContainerMetaData();
	}
	
	// private method to fill arraylists
	private void fetchContainerMetaData() {
		for(Container container: dockerClient.listContainersCmd().exec()){
	    	containerIDs.add(container.getId());
	    	containerResponses.add(dockerClient.inspectContainerCmd(container.getId()).exec());
	    }
	}
	
	// get the metadata of a single instance of the containers running on the host as json object
	// the instance is specified by id
	private InspectContainerResponse getRunningContainerResponse(String id) {
		if(this.getRunningSize() > 0) {
			for(InspectContainerResponse containerResponse: containerResponses){
		    	if(containerResponse.getId().equals(id)) {
		    		return containerResponse;
		    	}
		    }
		}
		return null;
	}	
	
	// get a String list of all currently running ids of containers on the host
	public List<String> getIDs() {
		return containerIDs;
	}
	
	// the same as String
	public JSONArray getJsonIDs() {
		return new JSONArray(containerIDs);
	}
	
	// get the number of running containers on the host
	public int getRunningSize() {
		return containerIDs.size();
	}

	public List<String> getRunningNames() {
		List<String> ret = new ArrayList<String>();
		if(this.getRunningSize() > 0) {
			for(InspectContainerResponse response: containerResponses){
		    	ret.add(response.getName());
		    }
		}
		return ret;
	}
	
	// get the metadata of all running containers on the host as JSONObject
	public JSONArray getJsonMetaData() {
		JSONArray ret = new JSONArray();
		if(this.getRunningSize() > 0) {
			for(InspectContainerResponse containerResponse: containerResponses){
		    	ret.put(new JSONObject(containerResponse));
		    }
		}
		return ret;
	}
	
	// get the metadata of a single instance of the containers running on the host as JSONObject
	// the instance is specified by id
	public JSONObject getJsonMetaData(String id) {
		InspectContainerResponse reponse = this.getRunningContainerResponse(id);
		if(reponse != null) {
		    return new JSONObject(reponse);
		}
		return new JSONObject();
	}

	// get the metadata of a single instance of the containers running on the host as JSONObject
	// the instance is specified by id
	public JSONObject getJsonLabel(String id) {
		InspectContainerResponse reponse = this.getRunningContainerResponse(id);
		if(reponse != null) {
		    return new JSONObject(reponse.getConfig().getLabels());
		}
		return new JSONObject();
	}

	public JSONArray getJsonLabels() {
		JSONArray ret = new JSONArray();
		if(this.getRunningSize() > 0) {
			for(String id: this.getIDs()){
				JSONObject container = new JSONObject();
				container.put(id, this.getJsonLabel(id));
				ret.put(container);
		    }
		}
		return ret;
	}
}
 */
