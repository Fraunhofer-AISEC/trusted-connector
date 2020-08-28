import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.SecurityRequirements;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("Some test rely on external server resources and are somewhat unreliable right now.")
public class DapsDriverTest {

  @Test
  public void testValidToken() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertNotEquals(token, "INVALID_TOKEN");

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setRequiredSecurityLevel("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE")
        .build();

    SecurityRequirements requirements2 = new SecurityRequirements.Builder()
        .setRequiredSecurityLevel("idsc:BASE_CONNECTOR_SECURITY_PROFILE")
        .build();

    assertTrue(dapsDriver.verifyToken(token.getBytes(), requirements) >= 0);
    assertFalse(dapsDriver.verifyToken(token.getBytes(), requirements2) >= 0);
  }
  /****
  @Test
  public void testInvalidClient() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertEquals(token, "INVALID_TOKEN");
  }
   **/

  @Test
  public void testInvalidUrlNonSecure() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertEquals(token, "INVALID_TOKEN");
  }

  @Test
  public void testInvalidUrl404() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertEquals(token, "INVALID_TOKEN");
  }

  @Test (expected = RuntimeException.class)
  public void testInvalidPassword1() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("INVALID_PASSWORD")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    new DefaultDapsDriver(config);
  }

  @Test (expected = RuntimeException.class)
  public void testInvalidPassword2() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("INVALID_PASSWORD")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    new DefaultDapsDriver(config);
  }

  @Test (expected = RuntimeException.class)
  public void testInvalidKeyAlias() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("INVALID_ALIAS")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    new DefaultDapsDriver(config);
  }

  @Test
  public void testInvalidAuditLogging() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertNotEquals(token, "INVALID_TOKEN");

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setRequiredSecurityLevel("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")
        .build();

    assertTrue(dapsDriver.verifyToken(token.getBytes(), requirements) < 0);
  }

  public static void main(String[] args) {
    //get token
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.p12").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.p12").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    System.out.println(token);

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setRequiredSecurityLevel("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE")
        .build();

    long ret;
    if (0 > (ret = dapsDriver.verifyToken(token.getBytes(), requirements))) {
      System.out.println("failed");
    } else {
      System.out.println("success: " + ret);
    }
  }
}
