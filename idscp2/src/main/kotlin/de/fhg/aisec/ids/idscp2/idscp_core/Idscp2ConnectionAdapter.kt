package de.fhg.aisec.ids.idscp2.idscp_core

import org.slf4j.LoggerFactory

open class Idscp2ConnectionAdapter : Idscp2ConnectionListener {
    override fun onError(t: Throwable) {
        LOG.error("Error received in Idscp2ConnectionAdapter", t)
    }

    override fun onClose(connection: Idscp2Connection) {}

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ConnectionAdapter::class.java)
    }
}