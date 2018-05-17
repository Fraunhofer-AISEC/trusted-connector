package de.fhg.ids.comm.server;

public class Configuration {
	protected int port = 8080;
	protected String basePath = "/";
	
	public Configuration port(int port) {
		this.port = port;
		return this;
	}
	
	public Configuration basePath(String basePath) {
		this.basePath = basePath;
		return this;
	}
	

}
