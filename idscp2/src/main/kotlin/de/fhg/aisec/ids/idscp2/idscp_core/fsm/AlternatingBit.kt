package de.fhg.aisec.ids.idscp2.idscp_core.fsm

/**
 * Implementation of an alternating bit protocol for reliability
 * see (https://en.wikipedia.org/wiki/Alternating_bit_protocol)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

class AlternatingBit(value: Boolean = false) {
    enum class Bit {
        ZERO, ONE
    }

    private var bit: Bit

    init {
        if (value) {
            this.bit = Bit.ONE
        } else {
            this.bit = Bit.ZERO
        }
    }


    fun alternate() {
        bit = if (bit == Bit.ZERO) {
            Bit.ONE
        } else {
            Bit.ZERO
        }
    }

    fun asBoolean(): Boolean {
        return this.bit != Bit.ZERO
    }

}