package de.fhg.aisec.ids.idscp2.app_layer

import de.fraunhofer.iais.eis.ids.jsonld.Serializer

object Utils {
    val SERIALIZER: Serializer by lazy { Serializer() }
}