package de.fhg.ids.comm;

import java.security.cert.Certificate;

public class CertificatePair {

  private Certificate localCertificate = null;
  private Certificate remoteCertificate = null;

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
