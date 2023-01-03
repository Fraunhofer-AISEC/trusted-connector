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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package de.fhg.aisec.ids.webconsole.api

import de.fhg.aisec.ids.api.acme.AcmeClient
import de.fhg.aisec.ids.api.acme.AcmeTermsOfService
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.ApiController
import de.fhg.aisec.ids.webconsole.api.data.Cert
import de.fhg.aisec.ids.webconsole.api.data.EstCaCertRequest
import de.fhg.aisec.ids.webconsole.api.data.EstIdRequest
import de.fhg.aisec.ids.webconsole.api.data.Identity
import de.fhg.aisec.ids.webconsole.api.helper.ProcessExecutor
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import sun.security.pkcs.PKCS7
import sun.security.pkcs10.PKCS10
import sun.security.x509.X500Name
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing certificates in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/certs/<method>.
 *
 * @author Hamed Rasifard (hamed.rasifard@aisec.fraunhofer.de)
</method> */
@ApiController
@RequestMapping("/certs")
@Api(value = "Identities and Certificates", authorizations = [Authorization(value = "oauth2")])
class CertApi(@Autowired private val settings: Settings) {

    @Autowired(required = false)
    private var acmeClient: AcmeClient? = null

    @ApiOperation(value = "Starts ACME renewal over X509v3 certificates")
    @GetMapping("acme_renew/{target}")
    fun getAcmeCert(
        @ApiParam(value = "Identifier of the component to renew. Currently, the only valid value is __webconsole__")
        @PathVariable("target")
        target: String
    ) {
        val config = settings.connectorConfig
        if ("webconsole" == target && acmeClient != null) {
            acmeClient?.renewCertificate(
                FileSystems.getDefault().getPath("etc", "tls-webconsole"),
                URI.create(config.acmeServerWebcon),
                config.acmeDnsWebcon.trim { it <= ' ' }.split("\\s*,\\s*".toRegex()).toTypedArray(),
                config.acmePortWebcon
            )
        } else {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ACME renewal for services other than WebConsole is not yet implemented!"
            )
        }
    }

    @ApiOperation(
        value = "Retrieves the Terms of Service (tos) of the ACME endpoint",
        response = AcmeTermsOfService::class
    )
    @GetMapping("/acme_tos")
    fun getAcmeTermsOfService(
        @ApiParam(value = "URI to retrieve the TOS from")
        @RequestParam
        uri: String
    ): AcmeTermsOfService? {
        return acmeClient?.getTermsOfService(URI.create(uri.trim()))
    }

    @GetMapping("list_certs", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "List installed certificates from trust store.",
        notes = "Certificates in this list refer to public keys that are trusted by this connector."
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "List of certificates"),
        ApiResponse(code = 500, message = "_Truststore not found_: If no trust store available")
    )
    fun listCerts(): List<Cert> {
        val truststoreFile = getKeystoreFile(settings.connectorConfig.truststoreName)
        return getKeystoreEntries(truststoreFile)
    }

    @GetMapping("list_identities", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "List installed certificates from the private key store.",
        notes = "Certificates in this list refer to private keys that can be used as identities by the connector."
    )
    fun listIdentities(): List<Cert> {
        val keystoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        return getKeystoreEntries(keystoreFile)
    }

    @PostMapping("create_identity", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.TEXT_PLAIN])
    fun createIdentity(
        @ApiParam(value = "Specification of the identity to create a key pair for")
        @RequestBody
        spec: Identity
    ): String {
        val alias = UUID.randomUUID().toString()
        try {
            doGenKeyPair(
                alias,
                spec,
                "RSA",
                4096,
                "SHA256WITHRSA",
                getKeystoreFile(settings.connectorConfig.keystoreName)
            )
        } catch (e: Exception) {
            throw throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message, e)
        }
        return alias
    }

    /** Delete a private/public key pair.  */
    @PostMapping("delete_identity", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Deletes a public/private key pair")
    fun deleteIdentity(@RequestBody alias: String): String {
        val keyStoreFile = getKeystoreFile(settings.connectorConfig.keystoreName)
        return if (delete(alias, keyStoreFile)) {
            alias
        } else {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /** Deletes a trusted certificate.  */
    @PostMapping("delete_cert", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Deletes a trusted certificate")
    fun deleteCert(@RequestBody alias: String): String {
        val keyStoreFile = getKeystoreFile(settings.connectorConfig.truststoreName)
        return if (delete(alias, keyStoreFile)) {
            alias
        } else {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    // @PostMapping("/install_trusted_cert", produces = [MediaType.TEXT_HTML], consumes = [MediaType.MULTIPART_FORM_DATA])
    // @ApiOperation(value = "Installs a new trusted public key certificate.")
    // @ApiImplicitParams(ApiImplicitParam(dataType = "java.io.File", name = "attachment", paramType = "formData"))
    //     // @Throws(
    //     IOException::class
    // )
    // fun installTrustedCert(
    //     @ApiParam(hidden = true, name = "attachment")
    //     @Multipart("upfile")
    //     attachment: Attachment
    // ): String {
    //     val filename = attachment.contentDisposition.getParameter("filename")
    //     val tempPath = File.createTempFile(filename, "cert")
    //     FileOutputStream(tempPath).use { out ->
    //         attachment.getObject(InputStream::class.java).use { inputStream ->
    //             inputStream.copyTo(out)
    //         }
    //     }
    //     val trustStoreName = settings.connectorConfig.truststoreName
    //     val success = storeCert(getKeystoreFile(trustStoreName), tempPath)
    //     if (success) {
    //         if (!tempPath.delete()) {
    //             LOG.warn("Failed to delete temporary file $tempPath")
    //         }
    //         return "Trusted certificate has been uploaded to $trustStoreName"
    //     }
    //     return "Error: certificate has NOT been uploaded to $trustStoreName"
    // }

    @PostMapping("/est_ca_certs", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.TEXT_PLAIN])
    @ApiOperation(
        value = "Get CA certificate from EST",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "Error fetching CA certificate via EST")
    )
    suspend fun requestEstCaCerts(@RequestBody request: EstCaCertRequest): String {
        val ucUrl = "${request.url}/.well-known/est/cacerts"
        val response = insecureHttpClient.get(ucUrl)
        if (response.status.value !in 200..299) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch root certificate, error code ${response.status.value}"
            )
        }
        val res = response.bodyAsText()
        val encoded = Base64.getDecoder().decode(res.replace(WHITESPACE_REGEX, ""))
        val certs = PKCS7(encoded).certificates
        val certHash = sha256Hash(certs[0])

        return if (certHash == request.hash) {
            certs.joinToString("\n") {
                val s = Base64.getMimeEncoder(64, "\n".toByteArray()).encode(it.encoded).decodeToString()
                "-----BEGIN CERTIFICATE-----\n$s\n-----END CERTIFICATE-----"
            }
        } else {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Hash check for EST root failed, expected was ${request.hash}, actual hash is $certHash."
            )
        }
    }

    /**
     * Convert byte to hexadecimal chars without any dependencies to libraries.
     * @param num Byte to get hexadecimal representation for
     * @return The hexadecimal representation of the given byte value
     */
    private fun byteToHex(num: Int): String {
        val hexDigits = CharArray(2)
        hexDigits[0] = Character.forDigit(num shr 4 and 0xF, 16)
        hexDigits[1] = Character.forDigit(num and 0xF, 16)
        return String(hexDigits)
    }

    private val hexLookup = HashMap<Byte, String>()

    /**
     * Encode a byte array to a hex string
     * @param byteArray Byte array to get hexadecimal representation for
     * @return Hexadecimal representation of the given bytes
     */
    private fun encodeHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") { hexLookup.computeIfAbsent(it) { num: Byte -> byteToHex(num.toInt()) } }
    }

    private fun sha256Hash(certificate: Certificate): String {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(certificate.encoded)
        val digest = sha256.digest()
        return encodeHexString(digest).lowercase()
    }

    @PostMapping("/store_est_ca_cert", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Store est CA certificate",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "No certificate found")
    )
    fun storeEstCACerts(@RequestBody certificates: String): Boolean {
        return certificates.split("-----END CERTIFICATE-----").map {
            it.replace(CLEAR_PEM_REGEX, "")
        }.filter { it.isNotEmpty() }.map { c ->
            val trustStoreName = settings.connectorConfig.truststoreName
            storeCertFromString(getKeystoreFile(trustStoreName), c)
        }.all { it }
    }

    @PostMapping("/request_est_identity", consumes = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Get CA certificate from EST",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "No certificate found")
    )
    suspend fun requestEstIdentity(@RequestBody r: EstIdRequest) {
        LOG.debug("Requesting certificate over EST...")
        LOG.debug("Step 1 - generate Keys")
        KeyPairGenerator.getInstance("RSA").apply { initialize(4096) }.generateKeyPair().let { keys ->
            LOG.debug("Step 2 - generate CSR")
            generatePKCS10(keys).let { csr ->
                LOG.debug("Step 3 - send requests")
                sendEstIdReq(r, csr).let { pkcs7 ->
                    LOG.debug("Step 4 - extract certificate")
                    pkcs7.certificates.firstOrNull { it.publicKey == keys.public }?.let {
                        LOG.debug("Step 5 - save certificate")
                        storeEstId(keys.private, it, r.alias)
                    }
                }
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun generatePKCS10(
        keys: KeyPair
    ): ByteArray {
        val sigAlg = "SHA256WithRSA"
        val pkcs = PKCS10(keys.public)
        // val signature: Signature = Signature.getInstance(sigAlg).apply { initSign(keys.private) }
        val principal = X500Principal("CN=common, OU=unit, O=org, C=US, L=location, S=state, EMAIL=a@mail.org")
        val x500name = X500Name(principal.encoded)
        pkcs.encodeAndSign(x500name, keys.private, sigAlg)
        ByteArrayOutputStream().use { bs ->
            PrintStream(bs).use { pkcs.print(it) }
            return bs.toByteArray()
        }
    }

    private suspend fun sendEstIdReq(r: EstIdRequest, csr: ByteArray): PKCS7 {
        val trustStoreName = settings.connectorConfig.truststoreName
        val trustStoreFile = getKeystoreFile(trustStoreName)
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also { tmf ->
            KeyStore.getInstance("pkcs12").also {
                FileInputStream(trustStoreFile).use { fis ->
                    it.load(fis, KEYSTORE_PWD.toCharArray())
                    tmf.init(it)
                }
            }
        }.trustManagers
        val secureHttpClient = HttpClient(Java) {
            engine {
                config {
                    sslContext(
                        SSLContext.getInstance("TLS").apply {
                            init(null, trustManagers, null)
                        }
                    )
                }
            }
            install(ContentNegotiation) {
                jackson()
            }
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(
                            // We need a username here, but effectively only password is checked
                            username = "username",
                            password = r.iet
                        )
                    }
                }
            }
        }
        val ucUrl = "${r.estUrl}/.well-known/est/simpleenroll"
        val pkcs10String = String(csr, StandardCharsets.UTF_8).replace(CLEAR_PEM_REGEX, "")
        val response: HttpResponse = secureHttpClient.post(ucUrl) {
            setBody(pkcs10String)
            headers {
                append("Content-Type", "application/pkcs10")
                append("Content-Transfer-Encoding", "base64")
            }
        }

        if (response.status.value !in 200..299) {
            throw RuntimeException("Failed to fetch certificate: ${response.status}\n${response.bodyAsText()}")
        }
        val res = response.bodyAsText()
        val encoded = Base64.getDecoder().decode(res.replace(WHITESPACE_REGEX, ""))
        return PKCS7(encoded)
    }

    private fun storeEstId(key: PrivateKey, cert: Certificate, alias: String): Boolean {
        val keyStoreName = settings.connectorConfig.keystoreName
        return storeCertFromString(
            getKeystoreFile(keyStoreName),
            Base64.getEncoder().encode(cert.encoded).decodeToString(),
            key,
            alias
        )
    }

    /** Stores a certificate in a JKS truststore.  */
    private fun storeCert(trustStoreFile: File, certFile: File): Boolean {
        val alias = certFile.name.replace(".", "_")
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            val certStream = fullStream(certFile.absolutePath)
            val certs = cf.generateCertificate(certStream)
            val keystore = KeyStore.getInstance("pkcs12")
            val password = KEYSTORE_PWD
            FileInputStream(trustStoreFile).use { fis ->
                keystore.load(fis, password.toCharArray())
            }
            // Add the certificate
            keystore.setCertificateEntry(alias, certs)
            FileOutputStream(trustStoreFile).use { fos ->
                keystore.store(fos, password.toCharArray())
            }
            true
        } catch (e: Exception) {
            LOG.error(e.message, e)
            false
        }
    }

    private fun storeCertFromString(
        trustStoreFile: File,
        cert: String,
        key: PrivateKey? = null,
        alias: String? = null
    ): Boolean {
        val encoded = Base64.getDecoder().decode(cert.replace(WHITESPACE_REGEX, ""))
        val cf = CertificateFactory.getInstance("X.509")
        val c = cf.generateCertificate(ByteArrayInputStream(encoded)) as X509Certificate
        return try {
            val keystore = KeyStore.getInstance("pkcs12")
            val password = KEYSTORE_PWD.toCharArray()
            FileInputStream(trustStoreFile).use { fis ->
                keystore.load(fis, password)
            }
            val entryAlias = alias ?: c.subjectX500Principal.name.let { name ->
                name.split(",").map { it.split("=") }.firstOrNull { it[0] == "CN" }?.get(1) ?: name
            }
            if (key == null) {
                // Add a CA certificate
                keystore.setCertificateEntry(entryAlias, c)
            } else {
                // Add an identity certificate with private key
                keystore.setKeyEntry(entryAlias, key, password, arrayOf(c))
            }
            FileOutputStream(trustStoreFile).use { fos ->
                keystore.store(fos, password)
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
                val keystore = KeyStore.getInstance("pkcs12")
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
                            for (entry in dn.split(",").toTypedArray()) {
                                val kv = entry.split("=").toTypedArray()
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
                val keystore = KeyStore.getInstance("pkcs12")
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
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val CLEAR_PEM_REGEX = Regex("\\s+|-----(?:BEGIN|END) [A-Z ]+-----")

        private val insecureHttpClient = HttpClient(Java) {
            engine {
                config {
                    sslContext(
                        SSLContext.getInstance("TLS").apply {
                            init(
                                null,
                                arrayOf(object : X509TrustManager {
                                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

                                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

                                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                                }),
                                null
                            )
                        }
                    )
                }
            }
            install(ContentNegotiation) {
                jackson()
            }
        }

        @Throws(IOException::class)
        private fun fullStream(fileName: String): InputStream {
            DataInputStream(FileInputStream(fileName)).use { dis ->
                val bytes = dis.readAllBytes()
                return ByteArrayInputStream(bytes)
            }
        }
    }
}
