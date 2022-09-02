/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.api

import de.fhg.aisec.ids.api.acme.AcmeClient
import de.fhg.aisec.ids.api.acme.AcmeTermsOfService
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.api.data.Cert
import de.fhg.aisec.ids.webconsole.api.data.Identity
import de.fhg.aisec.ids.webconsole.api.helper.ProcessExecutor
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.apache.cxf.jaxrs.ext.multipart.Attachment
import org.apache.cxf.jaxrs.ext.multipart.Multipart
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.UUID
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing certificates in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/certs/<method>.
 *
 * @author Hamed Rasifard (hamed.rasifard@aisec.fraunhofer.de)
</method> */
@Component
@Path("/certs")
@Api(value = "Identities and Certificates", authorizations = [Authorization(value = "oauth2")])
class CertApi(@Autowired private val settings: Settings) {

    @Autowired(required = false)
    private var acmeClient: AcmeClient? = null

    @GET
    @ApiOperation(value = "Starts ACME renewal over X509v3 certificates")
    @Path("acme_renew/{target}")
    @AuthorizationRequired
    fun getAcmeCert(
        @ApiParam(value = "Identifier of the component to renew. Currently, the only valid value is __webconsole__")
        @PathParam("target")
        target: String
    ): Boolean {
        val config = settings.connectorConfig
        return if ("webconsole" == target && acmeClient != null) {
            acmeClient?.renewCertificate(
                FileSystems.getDefault().getPath("etc", "tls-webconsole"),
                URI.create(config.acmeServerWebcon),
                config.acmeDnsWebcon.trim { it <= ' ' }.split("\\s*,\\s*".toRegex()).toTypedArray(),
                config.acmePortWebcon
            )
            true
        } else {
            LOG.warn("ACME renewal for services other than WebConsole is not yet implemented!")
            false
        }
    }

    @GET
    @ApiOperation(
        value = "Retrieves the Terms of Service (tos) of the ACME endpoint",
        response = AcmeTermsOfService::class
    )
    @Path("acme_tos")
    @AuthorizationRequired
    fun getAcmeTermsOfService(
        @ApiParam(value = "URI to retrieve the TOS from")
        @QueryParam("uri")
        uri: String
    ): AcmeTermsOfService? {
        return acmeClient?.getTermsOfService(URI.create(uri.trim { it <= ' ' }))
    }

    @GET
    @Path("list_certs")
    @ApiOperation(
        value = "List installed certificates from trust store.",
        notes = "Certificates in this list refer to public keys that are trusted by this connector."
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "List of certificates"),
        ApiResponse(code = 500, message = "_Truststore not found_: If no trust store available")
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun listCerts(): List<Cert> {
        val truststoreFile = getKeystoreFile(settings.connectorConfig.truststoreName)
        return getKeystoreEntries(truststoreFile)
    }

    @GET
    @Path("list_identities")
    @ApiOperation(
        value = "List installed certificates from the private key store.",
        notes = "Certificates in this list refer to private keys that can be used as identities by the connector."
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun listIdentities(): List<Cert> {
        val keystoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        return getKeystoreEntries(keystoreFile)
    }

    @POST
    @Path("create_identity")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun createIdentity(
        @ApiParam(value = "Specification of the identity to create a key pair for") spec: Identity
    ): String {
        val alias = UUID.randomUUID().toString()
        try {
            doGenKeyPair(
                alias,
                spec,
                "RSA",
                2048,
                "SHA1WITHRSA",
                getKeystoreFile(settings.connectorConfig.keystoreName)
            )
        } catch (e: Exception) {
            throw InternalServerErrorException(e)
        }
        return alias
    }

    /** Delete a private/public key pair.  */
    @POST
    @Path("delete_identity")
    @ApiOperation(value = "Deletes a public/private key pair")
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun deleteIdentity(alias: String): String {
        val keyStoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        val success = delete(alias, keyStoreFile)
        return if (success) {
            alias
        } else {
            throw InternalServerErrorException()
        }
    }

    /** Deletes a trusted certificate.  */
    @POST
    @Path("delete_cert")
    @ApiOperation(value = "Deletes a trusted certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun deleteCert(alias: String): String {
        val keyStoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        val success = delete(alias, keyStoreFile)
        return if (success) {
            alias
        } else {
            throw InternalServerErrorException()
        }
    }

    @POST
    @Path("/install_trusted_cert")
    @ApiOperation(value = "Installs a new trusted public key certificate.")
    @ApiImplicitParams(ApiImplicitParam(dataType = "java.io.File", name = "attachment", paramType = "formData"))
    @Produces(
        MediaType.TEXT_HTML
    )
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @AuthorizationRequired
    @Throws(
        IOException::class
    )
    fun installTrustedCert(
        @ApiParam(hidden = true, name = "attachment")
        @Multipart("upfile")
        attachment: Attachment
    ): String {
        val filename = attachment.contentDisposition.getParameter("filename")
        val tempPath = File.createTempFile(filename, "cert")
        FileOutputStream(tempPath).use { out ->
            attachment.getObject(InputStream::class.java).use { `in` ->
                var read: Int
                val bytes = ByteArray(1024)
                while (`in`.read(bytes).also { read = it } != -1) {
                    out.write(bytes, 0, read)
                }
            }
        }
        val trustStoreName = settings.connectorConfig.truststoreName
        val success = storeCert(getKeystoreFile(trustStoreName), tempPath)
        if (success) {
            if (!tempPath.delete()) {
                LOG.warn("Failed to delete temporary file $tempPath")
            }
            return "Trusted certificate has been uploaded to $trustStoreName"
        }
        return "Error: certificate has NOT been uploaded to $trustStoreName"
    }

    /** Stores a certificate in a JKS truststore.  */
    private fun storeCert(trustStoreFile: File, certFile: File): Boolean {
        val cf: CertificateFactory
        val alias = certFile.name.replace(".", "_")
        return try {
            cf = CertificateFactory.getInstance("X.509")
            val certStream = fullStream(certFile.absolutePath)
            val certs = cf.generateCertificate(certStream)
            FileInputStream(trustStoreFile).use { fis ->
                FileOutputStream(trustStoreFile).use { fos ->
                    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                    val password = KEYSTORE_PWD
                    keystore.load(fis, password.toCharArray())

                    // Add the certificate
                    keystore.setCertificateEntry(alias, certs)
                    keystore.store(fos, password.toCharArray())
                }
            }
            true
        } catch (e: Exception) {
            LOG.error(e.message, e)
            false
        }
    }

    /**
     * Retrieve a keystore file.
     *
     *
     * This method will first try to load keystores from Karaf's ./etc dir, then checks if a path
     * has been given by -Dkeystore.dir=.. and finally just lets the classloader load the file from
     * classpath.
     *
     *
     * If the file cannot be found, this method returns null.
     */
    private fun getKeystoreFile(fileName: String): File {
        // We allow to override the default directory "etc" by specifying -Dkeystore.dir=...
        val keystoreDir = System.getProperty("keystore.dir", "etc")
        File(keystoreDir + File.separator + fileName).let {
            if (it.exists()) {
                return it
            }
        }

        File(fileName).let {
            if (it.exists()) {
                return it
            }
        }

        // Last resort: let the classloader find the file
        Thread.currentThread().contextClassLoader.getResource(fileName).let {
            try {
                if (it != null) {
                    return File(it.toURI())
                }
            } catch (e: URISyntaxException) {
                LOG.error(e.message, e)
            }
        }

        throw RuntimeException(
            "Keystore/truststore file could not be found. Cannot continue. Given filename: $fileName"
        )
    }

    /** Returns all entries (private keys and certificates) from a Java keystore.  */
    private fun getKeystoreEntries(keystoreFile: File): List<Cert> {
        try {
            FileInputStream(keystoreFile).use { fis ->
                val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                keystore.load(fis, KEYSTORE_PWD.toCharArray())
                val aliases = keystore.aliases().toList()
                return aliases
                    .mapNotNull {
                        val certificate = keystore.getCertificate(it)
                        if (certificate is X509Certificate) {
                            Pair(it, certificate)
                        } else {
                            null
                        }
                    }
                    .map { (alias, certificate) ->
                        Cert().also { cert ->
                            cert.alias = alias
                            cert.file = keystoreFile.name.replaceFirst("[.][^.]+$".toRegex(), "")
                            cert.certificate = certificate.toString()
                            cert.subjectAltNames = certificate.subjectAlternativeNames
                            // Get distinguished name
                            val dn = certificate.subjectX500Principal.name
                            for (entry in dn.split(",".toRegex()).toTypedArray()) {
                                val kv = entry.split("=".toRegex()).toTypedArray()
                                if (kv.size < 2) {
                                    continue
                                }
                                when (kv[0]) {
                                    "CN" -> cert.subjectCN = kv[1]
                                    "OU" -> cert.subjectOU = kv[1]
                                    "O" -> cert.subjectO = kv[1]
                                    "L" -> cert.subjectL = kv[1]
                                    "S" -> cert.subjectS = kv[1]
                                    "C" -> cert.subjectC = kv[1]
                                    else -> {
                                    }
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            throw e
        }
    }

    /**
     * We call the keystore binary programmatically. This is portable, in
     * contrast to creating key pairs and self-signed certificates
     * programmatically, which depends on internal classes of the JVM, such
     * as sun.security.* or oracle.*.
     */
    @Suppress("SameParameterValue")
    @Throws(InterruptedException::class, IOException::class)
    private fun doGenKeyPair(
        alias: String,
        spec: Identity,
        keyAlgName: String,
        keySize: Int,
        sigAlgName: String,
        keyStoreFile: File
    ) {
        val keytoolCmd = arrayOf(
            "/bin/sh",
            "-c",
            "keytool",
            "-genkey",
            "-alias",
            alias,
            "-keyalg",
            keyAlgName,
            "-keysize",
            keySize.toString(),
            "-sigalg",
            sigAlgName,
            "-keystore",
            keyStoreFile.absolutePath,
            "-dname",
            "CN=" + spec.cn + ", OU=" + spec.ou + ", O=" + spec.o + ", L=" + spec.l + ", S=" + spec.s +
                ", C=" + spec.c,
            "-storepass",
            KEYSTORE_PWD,
            "-keypass",
            KEYSTORE_PWD
        )
        val bos = ByteArrayOutputStream()
        ProcessExecutor().execute(keytoolCmd, bos, bos)
        LOG.debug("Keytool:\n\n{}", bos.toString(StandardCharsets.UTF_8))
    }

    private fun delete(alias: String, file: File): Boolean {
        try {
            FileInputStream(file).use { fis ->
                val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                keystore.load(fis, KEYSTORE_PWD.toCharArray())
                if (keystore.containsAlias(alias)) {
                    keystore.deleteEntry(alias)
                    FileOutputStream(file).use { keystore.store(it, KEYSTORE_PWD.toCharArray()) }
                } else {
                    LOG.warn("Alias not available. Cannot delete it: $alias")
                }
            }
        } catch (e: Exception) {
            LOG.error(e.message, e)
            return false
        }
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CertApi::class.java)
        private const val KEYSTORE_PWD = "password"

        @Throws(IOException::class)
        private fun fullStream(fileName: String): InputStream {
            DataInputStream(FileInputStream(fileName)).use { dis ->
                val bytes = dis.readAllBytes()
                return ByteArrayInputStream(bytes)
            }
        }
    }
}
