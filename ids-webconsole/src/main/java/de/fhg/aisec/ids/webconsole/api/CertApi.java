package de.fhg.aisec.ids.webconsole.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	@Path("list")
	@Produces("application/json")
	public String list() {
		List<Cert> certs = new ArrayList<>();
		List<File> files = new ArrayList<>();

		File file = getKeystoreFile("client-keystore.jks");
		if (file!=null) {
			files.add(file);
		}
		file = getKeystoreFile("client-truststore.jks");
		if (file!=null) {
			files.add(file);			
		}

		for (File f : files) {
			try (FileInputStream fis = new FileInputStream(f);) {
				KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				String password = KEYSTORE_PWD;
				keystore.load(fis, password.toCharArray());

				Enumeration<String> enumeration = keystore.aliases();
				while (enumeration.hasMoreElements()) {
					String alias = enumeration.nextElement();
					Certificate certificate = keystore.getCertificate(alias);
					Cert cert = new Cert();
					cert.alias = alias;
					cert.file = f.getName().replaceFirst("[.][^.]+$", "");					
					cert.certificate = certificate.toString();
					if (certificate instanceof X509Certificate) {
						X509Certificate c = (X509Certificate) certificate;
						cert.subjectAltNames = c.getSubjectAlternativeNames();
						// Get distinguished name
						String dn = c.getSubjectX500Principal().getName();
						for (String entry: dn.split(",")) {
							String[] kv = entry.split("=");
							switch (kv[0]) {
								case "CN": cert.subjectCN = kv[1];
									break;
								case "OU": cert.subjectOU = kv[1]; 
									break;
								case "O": cert.subjectO = kv[1]; 
									break;
								case "L": cert.subjectL = kv[1]; 
									break;
								case "S": cert.subjectS = kv[1]; 
									break;
								case "C": cert.subjectC = kv[1]; 
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
		}

		return new GsonBuilder().create().toJson(certs);
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
		try (	FileInputStream fis = new FileInputStream(keyStoreFile);
				FileOutputStream fos = new FileOutputStream(keyStoreFile);	) {
			
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			String password = KEYSTORE_PWD;
			keystore.load(fis, password.toCharArray());

			keystore.deleteEntry(alias);

			keystore.store(fos, password.toCharArray());
			return new Gson().toJson(true);
		} catch (java.security.cert.CertificateException | NoSuchAlgorithmException | KeyStoreException
				| IOException e) {
			LOG.error(e.getMessage(), e);
			return new Gson().toJson(e.getMessage());
		}
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
	 * This method will first try to load keystores from Karaf's ./etc dir, 
	 * then checks if a path has been given by -Dkeystore.dir=.. and finally just lets the 
	 * classloader load the file from classpath.
	 * 
	 * If the file cannot be found, this method returns null.
	 * 
	 * @param fileName
	 * @return
	 */
	private File getKeystoreFile(String fileName) {
		// If we run in karaf platform, we expect the keystore to be in KARAF_BASE/etc
		String etcDir = System.getProperty("karaf.etc");
		if (etcDir != null) {
			File f = new File(etcDir + File.separator + fileName);
			if (f.exists()) {
				return f;
			}
		}

		// Otherwise, we allow setting the directory to search for by -Dkeystore.dir=...
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
			if (clFile!=null) {
				return new File(clFile.toURI());
			}
		} catch (URISyntaxException e) {
	        LOG.error(e.getMessage(), e);
		}
		
		return null;
	}

//	private void createSelfSignedCert(File keystoreFile) {
//		CertificateFactory factory = CertificateFactory.getInstance("X.509");
//		X509Certificate cert = (X509Certificate) factory.generateCertificate(input);
//		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());		
//		keystore.setCertificateEntry(alias, cert);
//	}
}
