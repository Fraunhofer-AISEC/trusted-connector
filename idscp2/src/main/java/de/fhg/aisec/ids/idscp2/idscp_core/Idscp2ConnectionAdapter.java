package de.fhg.aisec.ids.idscp2.idscp_core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Idscp2ConnectionAdapter implements Idscp2ConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ConnectionAdapter.class);

    @Override
    public void onError(Throwable t) {
        LOG.error("Error received in Idscp2ConnectionAdapter", t);
    }

    @Override
    public void onClose(Idscp2Connection connection) {
    }
}
