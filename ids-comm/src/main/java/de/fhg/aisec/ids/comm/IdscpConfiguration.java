package de.fhg.aisec.ids.comm;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;

import java.net.URI;

public interface IdscpConfiguration {
  IdsAttestationType getAttestationType();

  int getAttestationMask();

  CertificatePair getCertificatePair();

  URI getTrustedThirdPartyURI();
}
