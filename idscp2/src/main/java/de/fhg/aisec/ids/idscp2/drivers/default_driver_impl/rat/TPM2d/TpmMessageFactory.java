package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d;


import com.google.protobuf.ByteString;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.*;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.RemoteToTpm2d.Code;
import java.util.List;

public class TpmMessageFactory {

  static Tpm2dMessageWrapper getAttestationChallengeMessage(
      byte[] nonce,
      IdsAttestationType aType,
      int pcrIndices
  ) {
    return Tpm2dMessageWrapper.newBuilder().setRatChallenge(
        Tpm2dRatChallenge.newBuilder()
            .setAtype(aType)
            .setNonce(ByteString.copyFrom(nonce))
            .setPcrIndices(pcrIndices)
            .build()
    ).build();
  }

  static Tpm2dMessageWrapper getAttestationResponseMessage(
      IdsAttestationType aType,
      String hash_alg,
      ByteString quoted,
      ByteString signature,
      List<Pcr> pcrValues,
      ByteString certificate
  ) {
    return Tpm2dMessageWrapper.newBuilder().setRatResponse(
        Tpm2dRatResponse.newBuilder()
            .setAtype(aType)
            .setHashAlg(hash_alg)
            .setQuoted(quoted)
            .setSignature(signature)
            .addAllPcrValues(pcrValues)
            .setCertificate(certificate)
            .build()
    ).build();
  }

  static Tpm2dMessageWrapper getAttestationResponseMessage(
      Tpm2dToRemote response
  ) {
    return Tpm2dMessageWrapper.newBuilder().setRatResponse(
        Tpm2dRatResponse.newBuilder()
            .setAtype(response.getAtype())
            .setHashAlg(response.getHalg().name())
            .setQuoted(response.getQuoted())
            .setSignature(response.getSignature())
            .addAllPcrValues(response.getPcrValuesList())
            .setCertificate(response.getCertificate())
            .build()
    ).build();
  }

  static Tpm2dMessageWrapper getAttestationResultMessage(
      //IdsAttestationType aType,
      boolean result
  ) {
    return Tpm2dMessageWrapper.newBuilder().setRatResult(
        Tpm2dRatResult.newBuilder()
            //.setAtype(aType)
            .setResult(result)
            .build()
    ).build();
  }

  /*static Tpm2dMessageWrapper getAttestationRepositoryRequestMessage(
      IdsAttestationType aType,
      List<Pcr> pcrValues
  ) {
    return Tpm2dMessageWrapper.newBuilder().setRepositoryRequest(
        Tpm2dRepositoryRequest.newBuilder()
            .setAtype(aType)
            .addAllPcrValues(pcrValues)
            .build()
    ).build();
  }

  static Tpm2dMessageWrapper getAttestationRepositoryResponseMessage() {
    return Tpm2dMessageWrapper.newBuilder().setRepositoryResponse(
        Tpm2dRepositoryResponse.newBuilder()
            .build()
    ).build();
  }*/

  static RemoteToTpm2d getRemoteToTPM2dMessage(
      IdsAttestationType aType,
      byte[] hash,
      int pcrIndices
  ) {
    return RemoteToTpm2d.newBuilder()
        .setAtype(aType)
        .setQualifyingData(ByteString.copyFrom(hash))
        .setCode(Code.ATTESTATION_REQ)
        .setPcrs(pcrIndices)
        .build();
  }
}
