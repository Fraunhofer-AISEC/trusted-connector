package de.fhg.aisec.ids.webconsole.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
		List<String> files = new ArrayList<>();
		
		//String keyStoreLocation = System.getProperty("javax.net.ssl.keyStore");
		//String keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-keystore.jks";
		String keyStoreLocation = "/home/hamed/Desktop/client-keystore.jks";
		if (keyStoreLocation != null) 
			files.add(keyStoreLocation);
		
		//keyStoreLocation = System.getProperty("javax.net.ssl.truststore");
		//keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-truststore.jks";
		keyStoreLocation = "/home/hamed/Desktop/client-truststore.jks";
		if (keyStoreLocation != null) 
			files.add(keyStoreLocation);
		
		for (String fileLocation : files) {
			try {

				File file = new File(fileLocation);
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
		            cert.file = file.getName().replaceFirst("[.][^.]+$", "");
		            cert.certificate = certificate.toString();
		            
		            certs.add(cert);
		        }

		    } catch (java.security.cert.CertificateException e) {
		        
		    } catch (NoSuchAlgorithmException e) {
		       
		    } catch (FileNotFoundException e) {
		        
		    } catch (KeyStoreException e) {
		        
		    } catch (IOException e) {
		        
		    }
        }
		
	    return new GsonBuilder().create().toJson(certs);
	}
	
	/**
	 * Delete an entry in keystore file based on its alias.
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	@GET
	@Path("delete")
	@Produces("application/json")
	public String delete(@QueryParam("alias") String alias, @QueryParam("file") String file) {
		
		String keyStoreLocation = null;
		
		if(file.equals("client-keystore")) {
			//keyStoreLocation = System.getProperty("javax.net.ssl.keyStore");
			//keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-keystore.jks";
			keyStoreLocation = "/home/hamed/Desktop/client-keystore.jks";
		
		} else if (file.equals("client-truststore")) {
			//keyStoreLocation = System.getProperty("javax.net.ssl.truststore");
			//keyStoreLocation = System.getProperty("user.dir") + "/../../../openssl_cert_generation/client-truststore.jks";
			keyStoreLocation = "/home/hamed/Desktop/client-truststore.jks";
			
		}
		
		try {

			File keyStoreFile = new File(keyStoreLocation);
        	FileInputStream fis = new FileInputStream(keyStoreFile);
	        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = "password";
	        keystore.load(fis, password.toCharArray());
	        
	        keystore.deleteEntry(alias);
	        
	        FileOutputStream fos = new FileOutputStream(keyStoreFile);
	        keystore.store(fos, password.toCharArray());
	        return new Gson().toJson(true);
	        
		} catch (java.security.cert.CertificateException e) {
	       return new Gson().toJson(e.getMessage());
	    } catch (NoSuchAlgorithmException e) {
	       return new Gson().toJson(e.getMessage());
	    } catch (FileNotFoundException e) {
	       return new Gson().toJson(e.getMessage());
	    } catch (KeyStoreException e) {
	       return new Gson().toJson(e.getMessage());
	    } catch (IOException e) {
	       return new Gson().toJson(e.getMessage());
	    }
	}
	
	public class Cert {
		public String alias;
		public String file;
		public String certificate;
	}
}
