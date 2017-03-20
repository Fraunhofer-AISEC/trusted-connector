package de.fhg.aisec.ids.webconsole.api;

import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.webconsole.api.helper.ProcessExecutor;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import java.io.*;
 
/**
 * REST API interface for managing certificates in the connector.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/certs/<method>.
 * 
 * @author Hamed Rasifard (hamed.rasifard@aisec.fraunhofer.de)
 *
 */

@Path("/certs")
public class CertApi {
	private static final Logger LOG = LoggerFactory.getLogger(CertApi.class);
	private static final String KEYSTORE_PWD = "password";

	@GET
	@Path("list_certs")
	@Produces("application/json")
	public String listCerts() {
		List<Cert> certs = getKeystoreEntries(getKeystoreFile("client-truststore.jks"));
		return new GsonBuilder().create().toJson(certs);
	}

	@GET
	@Path("list_identities")
	@Produces("application/json")
	public String listIdentities() {
		List<Cert> certs = getKeystoreEntries(getKeystoreFile("client-keystore.jks"));
		return new GsonBuilder().create().toJson(certs);
	}
	
	@POST
	@Path("create_identity")
	@Produces("text/plain")
	public String createIdentity(IdentitySpec spec) {
		try {
			this.doGenKeyPair(UUID.randomUUID().toString(), spec, "RSA", 2048, "SHA1WITHRSA", getKeystoreFile("client-keystore.jks"));
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return "OK";
	}

	/**
	 * Delete an entry in keystore file based on its alias.
	 * 
	 */
	@GET
	@Path("delete")
	@Produces("application/json")
	public String delete(@QueryParam("alias") String alias, @QueryParam("file") String file) {
		if (!"client-keystore".equals(file) && !"client-truststore".equals(file)) {
			return new Gson().toJson("Invalid file");
		}
		
		File keyStoreFile = getKeystoreFile(file + ".jks");
		try (FileInputStream fis = new FileInputStream(keyStoreFile);){
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			String password = KEYSTORE_PWD;
	        keystore.load(fis, password.toCharArray());
	        
	        keystore.deleteEntry(alias);
	        
	        FileOutputStream fos = new FileOutputStream(keyStoreFile);
	        keystore.store(fos, password.toCharArray());
	        return new Gson().toJson(true);
		} catch (java.security.cert.CertificateException | NoSuchAlgorithmException | KeyStoreException
				| IOException e) {
			LOG.error(e.getMessage(), e);
			return new Gson().toJson(e.getMessage());
		}
	}
	
	
    
    @POST
    @Path("/upload")
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String uploadFile(@Multipart("keystoresFile") String keystoresFile, 
     @Multipart("upfile") Attachment attachment) throws IOException {

       String filename = attachment.getContentDisposition().getParameter("filename");

       String tempPath = "/tmp/" + filename;
       OutputStream out = new FileOutputStream(new File(tempPath));
        
       InputStream in = attachment.getObject(InputStream.class);

       int read = 0;
       byte[] bytes = new byte[1024];
       while ((read = in.read(bytes)) != -1) {
           out.write(bytes, 0, read);
       }
       in.close();
       out.flush();
       out.close();
       
       if(addKey(keystoresFile, tempPath)) {
    	   return "certification has been uploaded to " + keystoresFile;
       }
       
       return "Error: certification has NOT been uploaded to " + keystoresFile;
    }      
    
    private boolean addKey(String dest, String filePath) {
    	
    	boolean returnResult = false;
    	CertificateFactory cf;
    	InputStream certstream;
    	String alias = filePath.substring(filePath.lastIndexOf("/")+1, filePath.length()).replaceFirst("[.][^.]+$", "");
    	File keyStoreFile = getKeystoreFile(dest + ".jks");
		try {
			cf = CertificateFactory.getInstance("X.509");
			certstream = fullStream(filePath);
			Certificate certs =  cf.generateCertificate(certstream);
			
			FileInputStream fis = new FileInputStream(keyStoreFile);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			String password = KEYSTORE_PWD;
		    keystore.load(fis, password.toCharArray());
		    
		    // Add the certificate
		    keystore.setCertificateEntry(alias, certs);
		    
		    FileOutputStream fos = new FileOutputStream(keyStoreFile);
	        keystore.store(fos, password.toCharArray());
	        
		    returnResult = true;
		    
		    try {
		    	File file = new File(filePath);
		    	file.delete();
		    } catch (Exception e) {
				e.printStackTrace();
			}
			
		} catch ( CertificateException |
				KeyStoreException | 
				NoSuchAlgorithmException | 
				IOException e) {
			e.printStackTrace();
			returnResult = false;
		}
		
    
    	return returnResult;
    }
    
    private static InputStream fullStream ( String fname ) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        dis.close();
        return bais;
    }
    
	public class Cert {
		public String subjectC;
		public String subjectS;
		public String subjectL;
		public String subjectO;
		public String subjectOU;
		public Collection<List<?>> subjectAltNames;
		public String subjectCN;
		public String issuerDistinguishedName;
		public String alias;
		public String file;
		public String certificate;
	}

	/**
	 * Retrieve a keystore file.
	 * 
	 * This method will first try to load keystores from Karaf's ./etc dir, then
	 * checks if a path has been given by -Dkeystore.dir=.. and finally just
	 * lets the classloader load the file from classpath.
	 * 
	 * If the file cannot be found, this method returns null.
	 * 
	 * @param fileName
	 * @return
	 */
	private File getKeystoreFile(String fileName) {
		// If we run in karaf platform, we expect the keystore to be in
		// KARAF_BASE/etc
		String etcDir = System.getProperty("karaf.etc");
		if (etcDir != null) {
			File f = new File(etcDir + File.separator + fileName);
			if (f.exists()) {
				return f;
			}
		}

		// Otherwise, we allow setting the directory to search for by
		// -Dkeystore.dir=...
		String keystoreDir = System.getProperty("keystore.dir");
		if (keystoreDir != null) {
			File f = new File(keystoreDir + File.separator + fileName);
			if (f.exists()) {
				return f;
			}
		}

		// Last resort: let the classloader find the file
		URL clFile = this.getClass().getClassLoader().getResource(fileName);
		try {
			if (clFile != null) {
				return new File(clFile.toURI());
			}
		} catch (URISyntaxException e) {
			LOG.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Returns all entries (private keys and certificates) from a Java keystore.
	 * 
	 * @param keystoreFile
	 * @return
	 */
	private List<Cert> getKeystoreEntries(File keystoreFile) {
		List<Cert> certs = new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(keystoreFile);) {
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			String password = KEYSTORE_PWD;
			keystore.load(fis, password.toCharArray());

			Enumeration<String> enumeration = keystore.aliases();
			while (enumeration.hasMoreElements()) {
				String alias = enumeration.nextElement();
				Certificate certificate = keystore.getCertificate(alias);
				Cert cert = new Cert();
				cert.alias = alias;
				cert.file = keystoreFile.getName().replaceFirst("[.][^.]+$", "");
				cert.certificate = certificate.toString();
				if (certificate instanceof X509Certificate) {
					X509Certificate c = (X509Certificate) certificate;
					cert.subjectAltNames = c.getSubjectAlternativeNames();
					// Get distinguished name
					String dn = c.getSubjectX500Principal().getName();
					for (String entry : dn.split(",")) {
						String[] kv = entry.split("=");
						switch (kv[0]) {
						case "CN":
							cert.subjectCN = kv[1];
							break;
						case "OU":
							cert.subjectOU = kv[1];
							break;
						case "O":
							cert.subjectO = kv[1];
							break;
						case "L":
							cert.subjectL = kv[1];
							break;
						case "S":
							cert.subjectS = kv[1];
							break;
						case "C":
							cert.subjectC = kv[1];
							break;
						default:
							break;
						}
					}
				}

				certs.add(cert);
			}
		} catch (java.security.cert.CertificateException | KeyStoreException | NoSuchAlgorithmException
				| IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return certs;
	}

	private void doGenKeyPair(String alias, IdentitySpec spec, String keyAlgName, int keysize, String sigAlgName, File keyStoreFile) throws Exception {
		/* 
		 * We call the keystore binary programmatically. This is portable, in contrast to creating key pairs and 
		 * self-signed certificates programmatically, which depends on internal classes of the JVM, such as 
		 * sun.security.* or oracle.*.
		 */		
		String[] keytoolCmd = new String[] {"keytool", 
				"-genkey", 
				"-alias",  "replserver", 
				"-keyalg", "RSA", 
				"-keystore", keyStoreFile.getAbsolutePath(), 
				"-dname", "CN="+spec.cn+", OU="+spec.ou+", O="+spec.o+", L="+spec.l+", S="+ spec.s + ", C="+spec.c, 
				"-storepass", KEYSTORE_PWD, 
				"-keypass",  KEYSTORE_PWD};
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new ProcessExecutor().execute(keytoolCmd, bos, bos);
		LOG.debug("Keytool: " + new String(bos.toByteArray()));
	}
	
	public class IdentitySpec {
		public String cn;
		public String ou;
		public String o;
		public String l;
		public String s;
		public String c;
	}
}
