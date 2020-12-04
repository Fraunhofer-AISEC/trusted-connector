package de.fhg.aisec.ids.idscp2.idscp_core.error

class DatException : RuntimeException {
    constructor(s: String?) : super(s)
    constructor(s: String?, e: Exception?) : super(s, e)
}