package de.fhg.ids.attestation;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;

import de.fhg.aisec.ids.attestation.RemoteAttestationServer;
/**
 * Unit test for ratRepositoryTest
 */
public class RatRepositoryTest {
	
	static RemoteAttestationServer ratServer;
	Gson gson = new Gson();
	
	@BeforeClass
	public static void initRepo() {
		ratServer = new RemoteAttestationServer("127.0.0.1", "configurations/check", 31337);
	}
	
	@AfterClass
	public static void closeRepo() {
		ratServer = null;
	}	

	@Test
    public void testURL() throws MalformedURLException{
        assertTrue(ratServer.getURI().toURL().toString().equals("http://127.0.0.1:31337/configurations/check"));
    }
	
	@Test
    public void testDatabaseIsRunning() throws MalformedURLException, SQLException{
        assertTrue(!ratServer.getDatabase().getConnection().isClosed());
    }
	
	@Test
    public void testDefaultConfiguration() throws MalformedURLException, SQLException{
		String result = "[{\"id\":1,\"name\":\"default_one\",\"type\":\"BASIC\"},{\"id\":2,\"name\":\"default_two\",\"type\":\"ADVANCED\"}]";
		assertTrue(ratServer.getDatabase().getConfigurationList().equals(result));
    }
	
	@Test
    public void testDefaulConfiguration1() throws MalformedURLException, SQLException{
		String result = "{\"id\":1,\"name\":\"default_one\",\"type\":\"BASIC\",\"values\":[{\"order\":0,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":1,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":2,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":3,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":4,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":5,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":6,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":7,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":8,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":9,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"}]}";
		assertTrue(gson.toJson(ratServer.getDatabase().getConfiguration(1)).equals(result));
    }
	
	@Test
    public void testDefaulConfiguration2() throws MalformedURLException, SQLException{
		String result = "{\"id\":2,\"name\":\"default_two\",\"type\":\"ADVANCED\",\"values\":[{\"order\":0,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":1,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":2,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":3,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":4,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":5,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":6,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":7,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":8,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":9,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":10,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":11,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":12,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":13,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":14,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":15,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":16,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":17,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":18,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":19,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":20,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":21,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":22,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"},{\"order\":23,\"value\":\"0000000000000000000000000000000000000000000000000000000000000000\"}]}";		
		assertTrue(gson.toJson(ratServer.getDatabase().getConfiguration(2)).equals(result));
    }
}
