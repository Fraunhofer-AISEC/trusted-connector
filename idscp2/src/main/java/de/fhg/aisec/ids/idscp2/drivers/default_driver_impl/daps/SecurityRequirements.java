package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps;


import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A Security-Requirements class using Builder pattern to store the connectors expected
 * security attributes e.g. Audit Logging
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class SecurityRequirements {

  private int auditLogging;

  public static class Builder {
   @NonNull private final SecurityRequirements requirements = new SecurityRequirements();

   @NonNull
   public Builder setAuditLogging(int auditLogging) {
     this.requirements.auditLogging = auditLogging;
     return this;
   }

   @NonNull
   public SecurityRequirements build() {
     return requirements;
   }
  }

  public int getAuditLogging() {
    return auditLogging;
  }
}
