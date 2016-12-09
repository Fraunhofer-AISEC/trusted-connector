package de.fhg.ids.attestation;

import java.net.MalformedURLException;

import de.fhg.aisec.ids.attestation.RemoteAttestationServer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName ){
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite(){
        return new TestSuite( AppTest.class );
    }

    /**
     * @throws MalformedURLException 
     */
    public void testURL() throws MalformedURLException{
    	RemoteAttestationServer ratServer = new RemoteAttestationServer("127.0.0.1", "check", 31337);
        assertTrue(ratServer.getURI().toURL().toString().equals("http://127.0.0.1:31337/check"));
        ratServer.stop();
        ratServer.destroy();
    }
}
