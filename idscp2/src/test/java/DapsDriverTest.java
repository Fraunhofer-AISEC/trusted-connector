import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.SecurityRequirements;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class DapsDriverTest {
  //toDo add further junit tests

  private String getExpiredToken() {
    return "";
  }

  @Test
  @Ignore
  public void testValidToken() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertNotEquals(token, "INVALID_TOKEN");

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setAuditLogging(2)
        .build();

    SecurityRequirements requirements2 = new SecurityRequirements.Builder()
        .setAuditLogging(1)
        .build();

    assertTrue(dapsDriver.verifyToken(token.getBytes(), requirements) >= 0);
    assertTrue(dapsDriver.verifyToken(token.getBytes(), requirements2) >= 0);
  }

  @Test
  @Ignore
  public void testInvalidClient() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("INVALID_CLIENT")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
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
  @Ignore
  public void testInvalidUrlNonSecure() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("http://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertEquals(token, "INVALID_TOKEN");
  }

  @Test
  @Ignore
  public void testInvalidUrl404() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de/token")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertEquals(token, "INVALID_TOKEN");
  }

  @Test (expected = RuntimeException.class)
  @Ignore
  public void testInvalidPassword1() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("INVALID_PASSWORD")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
  }

  @Test (expected = RuntimeException.class)
  @Ignore
  public void testInvalidPassword2() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("INVALID_PASSWORD")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
  }

  @Test (expected = RuntimeException.class)
  @Ignore
  public void testInvalidKeyAlias() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("INVALID_ALIAS")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
  }

  @Test
  @Ignore
  public void testInvalidAuditLogging() {
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    assertNotEquals(token, "INVALID_TOKEN");

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setAuditLogging(3) //token has supports only audit_logging 2
        .build();

    assertTrue(dapsDriver.verifyToken(token.getBytes(), requirements) < 0);
  }

  public static void main(String[] args) {
    //get token
    DefaultDapsDriverConfig config =
        new DefaultDapsDriverConfig.Builder()
            .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
            .setKeyStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStorePath(DapsDriverTest.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setKeyStorePassword("password")
            .setTrustStorePassword("password")
            .setKeyAlias("1")
            .setDapsUrl("https://daps.aisec.fraunhofer.de")
            .build();

    DapsDriver dapsDriver = new DefaultDapsDriver(config);
    String token = new String(dapsDriver.getToken());
    System.out.println(token);

    SecurityRequirements requirements = new SecurityRequirements.Builder()
        .setAuditLogging(2)
        .build();

    long ret;
    if (0 > (ret = dapsDriver.verifyToken(token.getBytes(), requirements))) {
      System.out.println("failed");
    } else {
      System.out.println("success: " + ret);
    }
  }
}
