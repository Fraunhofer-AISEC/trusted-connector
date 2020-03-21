package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps;


import org.checkerframework.checker.nullness.qual.NonNull;

public class SecurityRequirements {

  private int auditLogging;

  public static class Builder {
   @NonNull private SecurityRequirements requirements = new SecurityRequirements();

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
