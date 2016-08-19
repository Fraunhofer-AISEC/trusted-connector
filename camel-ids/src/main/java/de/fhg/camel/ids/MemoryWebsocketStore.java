package de.fhg.camel.ids;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryWebsocketStore extends ConcurrentHashMap<String, DefaultWebsocket> implements WebsocketStore {

    private static final long serialVersionUID = -2826843758230613922L;

    @Override
    public void add(String key, DefaultWebsocket ws) {
        super.put(key, ws);
    }

    @Override
    public void remove(DefaultWebsocket ws) {
        super.remove(ws.getConnectionKey());
    }

    @Override
    public void remove(String key) {
        super.remove(key);
    }

    @Override
    public DefaultWebsocket get(String key) {
        return super.get(key);
    }

    @Override
    public Collection<DefaultWebsocket> getAll() {
        return super.values();
    }

}