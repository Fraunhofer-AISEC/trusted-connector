package de.fhg.aisec.ids.camel.idscp2.processors

import de.fraunhofer.iais.eis.ids.jsonld.Serializer

object Utils {
    val SERIALIZER: Serializer by lazy { Serializer() }
}