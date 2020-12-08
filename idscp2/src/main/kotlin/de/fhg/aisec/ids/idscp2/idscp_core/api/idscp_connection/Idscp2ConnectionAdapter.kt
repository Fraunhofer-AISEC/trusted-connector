package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection

import org.slf4j.LoggerFactory

open class Idscp2ConnectionAdapter : Idscp2ConnectionListener {
    override fun onError(t: Throwable) {
        LOG.error("Error received in Idscp2ConnectionAdapter", t)
    }

    override fun onClose() {}

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ConnectionAdapter::class.java)
    }
}