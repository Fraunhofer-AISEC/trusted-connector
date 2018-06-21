/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.webconsole.api;

import de.fhg.aisec.ids.api.acme.AcmeTermsOfService;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.data.Cert;
import de.fhg.aisec.ids.webconsole.api.data.Identity;
import de.fhg.aisec.ids.webconsole.api.helper.ProcessExecutor;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

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
	private static final String TRUSTSTORE_FILE = "client-truststore.jks";
	private static final String KEYSTORE_FILE = "client-keystore.jks";

	@GET
	@Path("acme_renew/{target}")
	public void getAcmeCert(@PathParam("target") String target) {
		ConnectorConfig config = WebConsoleComponent.getSettingsOrThrowSUE().getConnectorConfig();
		if ("webconsole".equals(target)) {
			WebConsoleComponent.getAcmeClient().renewCertificate(
					FileSystems.getDefault().getPath("etc", "tls-webconsole"),
					URI.create(config.getAcmeServerWebcon()),
					config.getAcmeDnsWebcon().trim().split("\\s*,\\s*"), config.getAcmePortWebcon());
		} else {
			LOG.warn("ACME renewal for services other than WebConsole is not yet implemented!");
		}
	}

	@GET
	@Path("acme_tos")
	public AcmeTermsOfService getAcmeTermsOfService(@QueryParam("uri") String uri) {
		return WebConsoleComponent.getAcmeClient().getTermsOfService(URI.create(uri.trim()));
	}

	@GET
	@Path("list_certs")
	@Produces("application/json")
	public Response listCerts() {
		File truststore = getKeystoreFile(TRUSTSTORE_FILE);		
		if (truststore==null) {
			return Response.serverError().entity("Truststore not found").build();
		}
		List<Cert> certs = getKeystoreEntries(truststore);
		return Response.ok(certs).build();
	}

	@GET
	@Path("list_identities")
	@Produces("application/json")
	public List<Cert> listIdentities() {
		File keystoreFile = getKeystoreFile(KEYSTORE_FILE);
		return getKeystoreEntries(keystoreFile);
	}

	@POST
	@Path("create_identity")
	@Produces("application/json")
	@Consumes("application/json")
	public Response createIdentity(Identity spec) {
		String alias = UUID.randomUUID().toString();
		try {			
			this.doGenKeyPair(alias, spec, "RSA", 2048, "SHA1WITHRSA",
					getKeystoreFile(KEYSTORE_FILE));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.serverError().entity(e.getMessage()).build();
		}
		return Response.ok(alias).build();
	}

	/**
	 * Delete a private/public key pair.
	 */
	@POST
	@Path("delete_identity")
	@Produces("application/json")
	public Response deleteIdentity(String alias) {		
		File keyStoreFile = getKeystoreFile(KEYSTORE_FILE);
		boolean success = delete(alias, keyStoreFile);
		if (success) {
			return Response.ok(alias).build();
		} else {
			return Response.serverError().build();
		}
	}
	
	/**
	 * Deletes a trusted certificate.
	 */
	@POST
	@Path("delete_cert")
	@Produces("application/json")
	public Response deleteCert(String alias) {		
		File keyStoreFile = getKeystoreFile(TRUSTSTORE_FILE);
		boolean success = delete(alias, keyStoreFile);
		if (success) {
			return Response.ok(alias).build();
		} else {
			return Response.serverError().build();
		}
	}
	
	@POST
	@Path("/install_trusted_cert")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String installTrustedCert(@Multipart("upfile") Attachment attachment) throws IOException {

		String filename = attachment.getContentDisposition().getParameter("filename");
		File tempPath = File.createTempFile(filename, "cert");
		try (OutputStream out = new FileOutputStream(tempPath);
			 InputStream in = attachment.getObject(InputStream.class)) {
			int read;
			byte[] bytes = new byte[1024];
			while ((read = in.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
		}

		boolean success = storeCert(getKeystoreFile(TRUSTSTORE_FILE), tempPath);
		if (success) {
			if (!tempPath.delete()) {
				LOG.warn("Failed to delete temporary file " + tempPath);
			}
			return "Trusted certificate has been uploaded to " + TRUSTSTORE_FILE;
		}

		return "Error: certificate has NOT been uploaded to " + TRUSTSTORE_FILE;
	}

	/**
	 * Stores a certificate in a JKS truststore.
	 * 
	 * @param trustStoreFile
	 * @param certFile
	 * @return
	 */
	private boolean storeCert(File trustStoreFile, File certFile) {
		CertificateFactory cf;
		String alias = certFile.getName().replace(".", "_");
		try {
			cf = CertificateFactory.getInstance("X.509");
			InputStream certstream = fullStream(certFile.getAbsolutePath());
			Certificate certs = cf.generateCertificate(certstream);

			try (FileInputStream fis = new FileInputStream(trustStoreFile);
					FileOutputStream fos = new FileOutputStream(trustStoreFile)) {
				KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				String password = KEYSTORE_PWD;
				keystore.load(fis, password.toCharArray());

				// Add the certificate
				keystore.setCertificateEntry(alias, certs);

				keystore.store(fos, password.toCharArray());
			}

			return true;
		} catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
	}

	private static InputStream fullStream(String fname) throws IOException {
		FileInputStream fis = new FileInputStream(fname);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		dis.close();
		return bais;
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

		// In case of unit tests, we expect resources to be "somehow" available in current working dir
		File f = new File(fileName);
		if (f.exists()) {
			return f;
		}
		
		// Last resort: let the classloader find the file
		URL clFile = Thread.currentThread().getContextClassLoader().getResource(fileName);
		try {
			if (clFile != null) {
				return new File(clFile.toURI());
			}
		} catch (URISyntaxException e) {
			LOG.error(e.getMessage(), e);
		}

		LOG.warn("Keystore/truststore file could not be found. This will likely result in an error. " + fileName);
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
		try (FileInputStream fis = new FileInputStream(keystoreFile)) {
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(fis, KEYSTORE_PWD.toCharArray());
			
			Enumeration<String> enumeration = keystore.aliases();
			while (enumeration.hasMoreElements()) {
				String alias = enumeration.nextElement();
				Certificate certificate = keystore.getCertificate(alias);
				Cert cert = new Cert();
				cert.alias = alias;
				cert.file = keystoreFile.getName().replaceFirst("[.][^.]+$", "");
				cert.certificate = certificate.toString();
				if (!(certificate instanceof X509Certificate)) {
					continue;
				}

				X509Certificate c = (X509Certificate) certificate;
				cert.subjectAltNames = c.getSubjectAlternativeNames();
				// Get distinguished name
				String dn = c.getSubjectX500Principal().getName();
				for (String entry : dn.split(",")) {
					String[] kv = entry.split("=");
					if (kv.length<2) {
						continue;
					}
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

				certs.add(cert);
			}
		} catch (java.security.cert.CertificateException | KeyStoreException | NoSuchAlgorithmException
				| IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return certs;
	}

	private void doGenKeyPair(String alias, Identity spec, String keyAlgName, int keysize, String sigAlgName,
			File keyStoreFile) throws InterruptedException, IOException {
		/*
		 * We call the keystore binary programmatically. This is portable, in
		 * contrast to creating key pairs and self-signed certificates
		 * programmatically, which depends on internal classes of the JVM, such
		 * as sun.security.* or oracle.*.
		 */
		String[] keytoolCmd = new String[] { "keytool", "-genkey", "-alias", alias, "-keyalg", keyAlgName, "-keysize",
				Integer.toString(keysize), "-sigalg", sigAlgName, "-keystore",
				keyStoreFile.getAbsolutePath(), "-dname", "CN=" + spec.cn + ", OU=" + spec.ou + ", O=" + spec.o + ", L="
						+ spec.l + ", S=" + spec.s + ", C=" + spec.c,
				"-storepass", KEYSTORE_PWD, "-keypass", KEYSTORE_PWD };
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new ProcessExecutor().execute(keytoolCmd, bos, bos);
		LOG.debug("Keytool: " + new String(bos.toByteArray(), StandardCharsets.UTF_8));
	}

	protected boolean delete(String alias, File file) {
		try ( 	FileInputStream fis = new FileInputStream(file) ) {
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(fis, KEYSTORE_PWD.toCharArray());			
			if (keystore.containsAlias(alias)) {
				keystore.deleteEntry(alias);
				try (FileOutputStream fos = new FileOutputStream(file)) {
					keystore.store(fos, KEYSTORE_PWD.toCharArray());
				}
			} else {
				LOG.warn("Alias not available. Cannot delete it: " + alias);
			}
		} catch (java.security.cert.CertificateException | NoSuchAlgorithmException | KeyStoreException
				| IOException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}
		return true;
	}
}
