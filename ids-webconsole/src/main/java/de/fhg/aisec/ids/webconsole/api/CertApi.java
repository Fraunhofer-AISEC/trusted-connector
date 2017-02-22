package de.fhg.aisec.ids.webconsole.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * REST API interface for managing "certification" in the connector. 
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/certs/<method>.
 * 
 * @author Hamed Rasifard (hamed.rasifard@aisec.fraunhofer.de)
 *
 */

@Path("/certs")
public class CertApi {
	private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);
	
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {
		List<Cert> certs = new ArrayList<>();
		
		try {

			//String keyStoreLocation = System.getProperty("javax.net.ssl.keyStore");
			String keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-keystore.jks";
			File file = new File(keyStoreLocation);
	        FileInputStream fis = new FileInputStream(file);
	        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = "password";
	        keystore.load(fis, password.toCharArray());

	        Enumeration<String> enumeration = keystore.aliases();
	        while(enumeration.hasMoreElements()) {
	            String alias = (String)enumeration.nextElement();
	            Certificate certificate = keystore.getCertificate(alias);
	            Cert cert = new Cert();
	            cert.alias = alias;
	            cert.file = "client-keystore";
	            cert.certificate = certificate.toString();
	            
	            certs.add(cert);
	        }

	    } catch (java.security.cert.CertificateException e) {
	        e.printStackTrace();
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (KeyStoreException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
		try {

				//String keyStoreLocation = System.getProperty("javax.net.ssl.truststore");
				String keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-truststore.jks";
				File file = new File(keyStoreLocation);
	        	FileInputStream fis = new FileInputStream(file);
		        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		        String password = "password";
		        keystore.load(fis, password.toCharArray());

		        Enumeration<String> enumeration = keystore.aliases();
		        while(enumeration.hasMoreElements()) {
		            String alias = (String)enumeration.nextElement();
		            Certificate certificate = keystore.getCertificate(alias);
		            Cert cert = new Cert();
		            cert.alias = alias;
		            cert.file = "client-truststore";
		            cert.certificate = certificate.toString();
		            
		            certs.add(cert);
		        }

		    } catch (java.security.cert.CertificateException e) {
		        e.printStackTrace();
		    } catch (NoSuchAlgorithmException e) {
		        e.printStackTrace();
		    } catch (FileNotFoundException e) {
		        e.printStackTrace();
		    } catch (KeyStoreException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		 
	    return new GsonBuilder().create().toJson(certs);
	}
	
	@GET
	@Path("delete")
	@Produces("application/json")
	public String delete(@QueryParam("ceritficateId") String certificateId) {
		
		return new Gson().toJson(true);
	}
	
	public class Cert {
		public String alias;
		public String file;
		public String certificate;
	}
}
