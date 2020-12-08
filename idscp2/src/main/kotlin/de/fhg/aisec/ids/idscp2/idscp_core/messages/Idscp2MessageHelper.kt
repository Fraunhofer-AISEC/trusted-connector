package de.fhg.aisec.ids.idscp2.idscp_core.messages

import com.google.protobuf.ByteString
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.AlternatingBit
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.*
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause

/**
 * A factory for creating IDSCP2 messages
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
object Idscp2MessageHelper {
    fun createIdscpHelloMessage(dat: ByteArray, supportedRatSuite: Array<String>, expectedRatSuite: Array<String>): IdscpMessage {
        val idscpDat = IdscpDat.newBuilder()
                .setToken(ByteString.copyFrom(dat))
                .build()
        val idscpHello = IdscpHello.newBuilder()
                .setVersion(2)
                .setDynamicAttributeToken(idscpDat)
                .addAllExpectedRatSuite(listOf(*expectedRatSuite))
                .addAllSupportedRatSuite(listOf(*supportedRatSuite))
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpHello(idscpHello)
                .build()
    }

    fun createIdscpCloseMessage(closeMsg: String?, causeCode: CloseCause?): IdscpMessage {
        val idscpClose = IdscpClose.newBuilder()
                .setCauseCode(causeCode)
                .setCauseMsg(closeMsg)
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpClose(idscpClose)
                .build()
    }

    fun createIdscpDatExpiredMessage(): IdscpMessage {
        return IdscpMessage.newBuilder()
                .setIdscpDatExpired(IdscpDatExpired.newBuilder().build())
                .build()
    }

    fun createIdscpDatMessage(dat: ByteArray?): IdscpMessage {
        val idscpDat = IdscpDat.newBuilder()
                .setToken(ByteString.copyFrom(dat))
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpDat(idscpDat)
                .build()
    }

    fun createIdscpReRatMessage(cause: String?): IdscpMessage {
        val idscpReRat = IdscpReRat.newBuilder()
                .setCause(cause)
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpReRat(idscpReRat)
                .build()
    }

    fun createIdscpDataMessage(data: ByteArray?): IdscpMessage {
        val idscpData = IdscpData.newBuilder()
                .setData(ByteString.copyFrom(data))
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpData(idscpData)
                .build()
    }

    fun createIdscpDataMessageWithAltBit(data: ByteArray?, alternatingBit: AlternatingBit): IdscpMessage {
        val idscpData = IdscpData.newBuilder()
                .setData(ByteString.copyFrom(data))
                .setAlternatingBit(alternatingBit.asBoolean())
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpData(idscpData)
                .build()
    }

    fun createIdscpRatProverMessage(body: ByteArray?): IdscpMessage {
        val idscpRatProver = IdscpRatProver.newBuilder()
                .setData(ByteString.copyFrom(body))
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpRatProver(idscpRatProver)
                .build()
    }

    fun createIdscpRatVerifierMessage(body: ByteArray?): IdscpMessage {
        val idscpRatVerifier = IdscpRatVerifier.newBuilder()
                .setData(ByteString.copyFrom(body))
                .build()
        return IdscpMessage.newBuilder()
                .setIdscpRatVerifier(idscpRatVerifier)
                .build()
    }

    fun createIdscpAckMessage(alternatingBit: Boolean): IdscpMessage {
        return IdscpMessage.newBuilder()
                .setIdscpAck(
                        IdscpAck.newBuilder().setAlternatingBit(alternatingBit).build()
                ).build()
    }
}