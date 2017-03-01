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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
	static String zero = "0000000000000000000000000000000000000000000000000000000000000000";
	private static HttpURLConnection conn;
	private static int port = AvailablePortFinder.getNextAvailable();
	private static SSLContext sc;
	private static HostnameVerifier hv;
	private Logger LOG = LoggerFactory.getLogger(RatRepositoryTest.class);
	private String sURL = "https://127.0.0.1:"+port+"/configurations/check"; 
	
	@BeforeClass
	public static void initRepo() {
		ratServer = new RemoteAttestationServer("127.0.0.1", "configurations/check", port );
		ratServer.start();
		
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			System.out.println("Error" + e);
		}
        
        hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. "
                        + session.getPeerHost());
                return true;
            }
        };
        
	}
	
	@AfterClass
	public static void closeRepo() {
		ratServer = null;
	}	

	@Test
    public void testURL() throws MalformedURLException{
        assertTrue(ratServer.getURI().toURL().toString().equals(sURL));
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
    public void testBASICConfiguration() throws IOException {
		// this tests the BASIC pcr values configuration 0-10
		String qualifyingData = UUID.randomUUID().toString();
		String signature = "";
		String uri = "";
		long id = new java.util.Random().nextLong();
		values = new Pcr[10];
    	for(int i = 0; i < 10;i++) {
    		values[i] = Pcr.newBuilder()
    				.setNumber(i)
    				.setValue(zero)
    				.build();
    	}	
        ConnectorMessage msg = ConnectorMessage
        		.newBuilder()
        		.setId(id)
				.setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
        		.setAttestationRepositoryRequest(
		        		AttestationRepositoryRequest
		        		.newBuilder()
		        		.setAtype(IdsAttestationType.BASIC)
		        		.setQualifyingData(qualifyingData)
		        		.addAllPcrValues(Arrays.asList(values))
		        		.build()
		        ).build();    
        LOG.debug("-----------------------------------msg to repo:-------------------------------------");
        LOG.debug(msg.toString());
        HttpsURLConnection urlc = (HttpsURLConnection) new URL(sURL).openConnection();
        urlc.setHostnameVerifier(hv);
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        msg.writeTo(urlc.getOutputStream());
        ConnectorMessage result = ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
        LOG.debug("-----------------------------------answer from repo:-------------------------------------");
        LOG.debug(result.toString());
        assertTrue(result.getId() == id + 1);
        assertTrue(result.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE));
        assertTrue(result.getAttestationRepositoryResponse().getAtype().equals(IdsAttestationType.BASIC));
        assertTrue(result.getAttestationRepositoryResponse().getQualifyingData().equals(qualifyingData));
        assertTrue(result.getAttestationRepositoryResponse().getResult());
        assertTrue(result.getAttestationRepositoryResponse().getSignature().equals(signature));
        assertTrue(result.getAttestationRepositoryResponse().getCertificateUri().equals(uri));
    }
	
	@Test
    public void testALLConfiguration() throws IOException {
		// this tests the ALL pcr values configuration 0-23
		String qualifyingData = UUID.randomUUID().toString();
		String signature = "";
		String uri = "";
		long id = new java.util.Random().nextLong();
		values = new Pcr[24];
    	for(int i = 0; i < 24;i++) {
    		values[i] = Pcr.newBuilder()
    				.setNumber(i)
    				.setValue(zero)
    				.build();
    	}	
        ConnectorMessage msg = ConnectorMessage
        		.newBuilder()
        		.setId(id)
				.setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
        		.setAttestationRepositoryRequest(
		        		AttestationRepositoryRequest
		        		.newBuilder()
		        		.setAtype(IdsAttestationType.ALL)
		        		.setQualifyingData(qualifyingData)
		        		.addAllPcrValues(Arrays.asList(values))
		        		.build()
		        ).build();
        LOG.debug("-----------------------------------msg to repo:-------------------------------------");
        LOG.debug(msg.toString());
        HttpsURLConnection urlc = (HttpsURLConnection) new URL(sURL).openConnection();
        urlc.setHostnameVerifier(hv);
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        msg.writeTo(urlc.getOutputStream());
        ConnectorMessage result = ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
        LOG.debug("-----------------------------------answer from repo:-------------------------------------");
        LOG.debug(result.toString());
        assertTrue(result.getId() == id + 1);
        assertTrue(result.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE));
        assertTrue(result.getAttestationRepositoryResponse().getAtype().equals(IdsAttestationType.ALL));
        assertTrue(result.getAttestationRepositoryResponse().getQualifyingData().equals(qualifyingData));
        assertTrue(result.getAttestationRepositoryResponse().getResult());
        assertTrue(result.getAttestationRepositoryResponse().getSignature().equals(signature));
        assertTrue(result.getAttestationRepositoryResponse().getCertificateUri().equals(uri));
    }

	@Test
    public void testADVANCEDConfiguration() throws IOException {
		// this tests the ADVANCED pcr values configuration in this case with 5 values: PCR0, PCR2, PCR4, PCR6, PCR8
		String qualifyingData = UUID.randomUUID().toString();
		String signature = "";
		String uri = "";
		long id = new java.util.Random().nextLong();
		values = new Pcr[5];
    	for(int i = 0; i < 5;i++) {
    		values[i] = Pcr.newBuilder()
    				.setNumber(2*i)
    				.setValue(zero)
    				.build();
    	}	
        ConnectorMessage msg = ConnectorMessage
        		.newBuilder()
        		.setId(id)
				.setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
        		.setAttestationRepositoryRequest(
		        		AttestationRepositoryRequest
		        		.newBuilder()
		        		.setAtype(IdsAttestationType.ADVANCED)
		        		.setQualifyingData(qualifyingData)
		        		.addAllPcrValues(Arrays.asList(values))
		        		.build()
		        ).build();
        LOG.debug("-----------------------------------msg to repo:-------------------------------------");
        LOG.debug(msg.toString());
        HttpsURLConnection urlc = (HttpsURLConnection) new URL(sURL).openConnection();
        urlc.setHostnameVerifier(hv);
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setRequestMethod("POST");
        urlc.setRequestProperty("Accept", "application/x-protobuf");
        urlc.setRequestProperty("Content-Type", "application/x-protobuf");
        msg.writeTo(urlc.getOutputStream());
        ConnectorMessage result = ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
        LOG.debug("-----------------------------------answer from repo:-------------------------------------");
        LOG.debug(result.toString());
        assertTrue(result.getId() == id + 1);
        assertTrue(result.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE));
        assertTrue(result.getAttestationRepositoryResponse().getAtype().equals(IdsAttestationType.ADVANCED));
        assertTrue(result.getAttestationRepositoryResponse().getQualifyingData().equals(qualifyingData));
        assertTrue(result.getAttestationRepositoryResponse().getResult());
        assertTrue(result.getAttestationRepositoryResponse().getSignature().equals(signature));
        assertTrue(result.getAttestationRepositoryResponse().getCertificateUri().equals(uri));
    }
}
