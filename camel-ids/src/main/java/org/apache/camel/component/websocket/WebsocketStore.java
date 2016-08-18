package org.apache.camel.component.websocket;

import java.util.Collection;

public interface WebsocketStore {

	public void add(String key, DefaultWebsocket ws);
	public void remove(DefaultWebsocket ws);
	public void remove (String key);
	public DefaultWebsocket get(String key);
	public Collection<DefaultWebsocket> getAll();
}
