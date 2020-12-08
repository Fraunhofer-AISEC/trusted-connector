package de.fhg.aisec.ids.idscp2.default_drivers.rat.tpm2d

import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket

/**
 * A TPM2d Socket for communication with the Trusted Platform Module
 */
class TPM2dSocket(host: String?) : Socket(host, 9505) {
    private val `is`: DataInputStream = DataInputStream(this.inputStream)
    private val os: DataOutputStream = DataOutputStream(this.outputStream)

    @Throws(IOException::class)
    fun requestAttestation(request: Tpm2dAttestation.RemoteToTpm2d): Tpm2dAttestation.Tpm2dToRemote {
        // Write attestation request message
        val requestBytes = request.toByteArray()
        os.writeInt(requestBytes.size)
        os.write(requestBytes)
        // Read attestation result message
        val resultBytes = ByteArray(`is`.readInt())
        `is`.readFully(resultBytes)
        return Tpm2dAttestation.Tpm2dToRemote.parseFrom(resultBytes)
    }

}