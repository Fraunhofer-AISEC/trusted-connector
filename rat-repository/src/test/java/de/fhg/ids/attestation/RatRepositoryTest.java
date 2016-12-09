package de.fhg.ids.attestation;

import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.junit.Test;

import de.fhg.aisec.ids.attestation.RemoteAttestationServer;
/**
 * Unit test for ratRepositoryTest
 */
public class RatRepositoryTest {

	@Test
    public void testURL() throws MalformedURLException{
    	RemoteAttestationServer ratServer = new RemoteAttestationServer("127.0.0.1", "check", 31337);
        assertTrue(ratServer.getURI().toURL().toString().equals("http://127.0.0.1:31337/check"));
    }
}
