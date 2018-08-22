package de.fhg.ids.comm;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;

public interface IdscpConfiguration {
  IdsAttestationType getAttestationType();

  int getAttestationMask();

  CertificatePair getCertificatePair();
}
