package de.fhg.aisec.ids.comm;

import java.security.cert.Certificate;

public class CertificatePair {

  private Certificate localCertificate = null;
  private Certificate remoteCertificate = null;

  public CertificatePair() {}

  public CertificatePair(CertificatePair certificatePair) {
    this.localCertificate = certificatePair.localCertificate;
    this.remoteCertificate = certificatePair.remoteCertificate;
  }

  public Certificate getLocalCertificate() {
    return localCertificate;
  }

  public void setLocalCertificate(Certificate localCertificate) {
    this.localCertificate = localCertificate;
  }

  public Certificate getRemoteCertificate() {
    return remoteCertificate;
  }

  public void setRemoteCertificate(Certificate remoteCertificate) {
    this.remoteCertificate = remoteCertificate;
  }
}
