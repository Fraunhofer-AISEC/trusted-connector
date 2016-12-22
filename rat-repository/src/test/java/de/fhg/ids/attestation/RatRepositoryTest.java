package de.fhg.ids.attestation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.camel.test.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.attestation.RemoteAttestationServer;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryResponse;

/**
 * Unit test for ratRepositoryTest
 */
public class RatRepositoryTest {
	
	static RemoteAttestationServer ratServer;
	static Pcr[] values;
	static int numOfPcrValues = 10;
	static String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	private static HttpURLConnection conn;
	private static int port = AvailablePortFinder.getNextAvailable();
	private Logger LOG = LoggerFactory.getLogger(RatRepositoryTest.class);
	
	@BeforeClass
	public static void initRepo() {
		ratServer = new RemoteAttestationServer("127.0.0.1", "configurations/check", port );
		ratServer.start();
	}
	
	@AfterClass
	public static void closeRepo() {
		ratServer = null;
	}	

	@Test
    public void testURL() throws MalformedURLException{
        assertTrue(ratServer.getURI().toURL().toString().equals("http://127.0.0.1:"+port+"/configurations/check"));
    }
	
	@Test
    public void testDatabaseIsRunning() throws MalformedURLException, SQLException{
        assertTrue(!ratServer.getDatabase().getConnection().isClosed());
    }
	
	@Test
    public void testDefaultConfiguration() throws MalformedURLException, SQLException{
		Gson gson = new Gson();
		String result = "[{\"id\":1,\"name\":\"default_one\",\"type\":\"BASIC\",\"values\":[{\"bitField0_\":3,\"number_\":0,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":1,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":2,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":3,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":4,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":5,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":6,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":7,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":8,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":9,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}]},{\"id\":2,\"name\":\"default_two\",\"type\":\"ADVANCED\",\"values\":[{\"bitField0_\":3,\"number_\":0,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":1,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":2,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":3,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":4,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":5,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":6,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":7,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":8,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":9,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":10,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":11,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":12,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":13,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":14,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":15,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":16,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":17,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":18,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":19,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":20,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":21,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":22,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0},{\"bitField0_\":3,\"number_\":23,\"value_\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"memoizedIsInitialized\":1,\"unknownFields\":{\"fields\":{}},\"memoizedSize\":-1,\"memoizedHashCode\":0}]}]";
		assertTrue(gson.toJson(ratServer.getDatabase().getConfigurationList()).equals(result));
    }
	
	@Test
    public void testUsingURLConnection() throws IOException {
		String qualifyingData = "abc";
		String signature = "";
		String uri = "";
		long id = 12334324;
		values = new Pcr[numOfPcrValues];
    	for(int i = 0; i < numOfPcrValues;i++) {
    		values[i] = Pcr.newBuilder()
    				.setNumber(i)
    				.setValue(zero)
    				.build();
    	}	
        ConnectorMessage msg = ConnectorMessage
        		.newBuilder()
        		.setId(id++)
				.setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
        		.setAttestationRepositoryRequest(
		        		AttestationRepositoryRequest
		        		.newBuilder()
		        		.setAtype(IdsAttestationType.BASIC)
		        		.setQualifyingData(qualifyingData)
		        		.addAllPcrValues(Arrays.asList(values))
		        		.build()
		        ).build();    
        URL url = new URL("http://127.0.0.1:"+port+"/configurations/check");
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        msg.writeTo(urlc.getOutputStream());
        ConnectorMessage result = ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
        assertTrue(result.getId() == id);
        assertTrue(result.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE));
        assertTrue(result.getAttestationRepositoryResponse().getAtype().equals(IdsAttestationType.BASIC));
        assertTrue(result.getAttestationRepositoryResponse().getQualifyingData().equals(qualifyingData));
        assertTrue(result.getAttestationRepositoryResponse().getResult());
        assertTrue(result.getAttestationRepositoryResponse().getSignature().equals(signature));
        assertTrue(result.getAttestationRepositoryResponse().getCertificateUri().equals(uri));
        
    }
    
}
