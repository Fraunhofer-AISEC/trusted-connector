package de.fhg.aisec.ids.idscp2.default_drivers.rat.tpm2d

import com.google.protobuf.ByteString
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.*

/**
 * A message factory for creating TPM2d RAT messages
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
object TPM2dMessageFactory {
    fun getAttestationChallengeMessage(
            nonce: ByteArray,
            aType: IdsAttestationType,
            pcrIndices: Int
    ): Tpm2dMessageWrapper {
        return Tpm2dMessageWrapper.newBuilder().setRatChallenge(
                Tpm2dRatChallenge.newBuilder()
                        .setAtype(aType)
                        .setNonce(ByteString.copyFrom(nonce))
                        .setPcrIndices(pcrIndices)
                        .build()
        ).build()
    }

    fun getAttestationResponseMessage(
            aType: IdsAttestationType,
            hash_alg: String,
            quoted: ByteString,
            signature: ByteString,
            pcrValues: List<Pcr>,
            certificate: ByteString
    ): Tpm2dMessageWrapper {
        return Tpm2dMessageWrapper.newBuilder().setRatResponse(
                Tpm2dRatResponse.newBuilder()
                        .setAtype(aType)
                        .setHashAlg(hash_alg)
                        .setQuoted(quoted)
                        .setSignature(signature)
                        .addAllPcrValues(pcrValues)
                        .setCertificate(certificate)
                        .build()
        ).build()
    }

    fun getAttestationResponseMessage(
            response: Tpm2dToRemote
    ): Tpm2dMessageWrapper {
        return Tpm2dMessageWrapper.newBuilder().setRatResponse(
                Tpm2dRatResponse.newBuilder()
                        .setAtype(response.atype)
                        .setHashAlg(response.halg.name)
                        .setQuoted(response.quoted)
                        .setSignature(response.signature)
                        .addAllPcrValues(response.pcrValuesList)
                        .setCertificate(response.certificate)
                        .build()
        ).build()
    }

    fun getAttestationResultMessage( //IdsAttestationType aType,
            result: Boolean
    ): Tpm2dMessageWrapper {
        return Tpm2dMessageWrapper.newBuilder().setRatResult(
                Tpm2dRatResult.newBuilder() //.setAtype(aType)
                        .setResult(result)
                        .build()
        ).build()
    }

    fun getRemoteToTPM2dMessage(
            aType: IdsAttestationType,
            hash: ByteArray,
            pcrIndices: Int
    ): RemoteToTpm2d {
        return RemoteToTpm2d.newBuilder()
                .setAtype(aType)
                .setQualifyingData(ByteString.copyFrom(hash))
                .setCode(RemoteToTpm2d.Code.ATTESTATION_REQ)
                .setPcrs(pcrIndices)
                .build()
    }
}