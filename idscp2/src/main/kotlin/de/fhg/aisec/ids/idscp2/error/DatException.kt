package de.fhg.aisec.ids.idscp2.error

class DatException : RuntimeException {
    constructor(s: String?) : super(s) {}
    constructor(s: String?, e: Exception?) : super(s, e) {}
}