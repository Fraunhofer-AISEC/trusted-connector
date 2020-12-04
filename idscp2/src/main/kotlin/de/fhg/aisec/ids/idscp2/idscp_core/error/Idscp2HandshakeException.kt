package de.fhg.aisec.ids.idscp2.idscp_core.error

/**
 * IDSCP2 Exception
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2HandshakeException : RuntimeException {
    constructor(message: String?) : super(message)
}