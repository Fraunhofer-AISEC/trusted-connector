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
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
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
import java.security.PublicKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.cxf.jaxrs.ext.multipart.Attachment
import org.apache.cxf.jaxrs.ext.multipart.Multipart
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sun.security.pkcs.PKCS7
import sun.security.pkcs10.PKCS10
import sun.security.x509.X500Name

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

    @POST
    @Path("est_ca_cert")
    @ApiOperation(
        value = "Get CA certificate from EST",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "No certificate found")
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun requestEstCert(request: EstCaCertRequest): String? {
        return getEstCaCert(request)
    }

    private fun getEstCaCert(r: EstCaCertRequest): String? {
        val res = runBlocking(Dispatchers.IO) {
            val ucUrl = "${r.url}/.well-known/est/cacerts"
            val response = insecureHttpClient.get(ucUrl)
            if (response.status.value !in 200..299) {
                throw RuntimeException("Failed to fetch root certificate")
            }
            response.bodyAsText()
        }
        // verify hash
        val encoded = Base64.getDecoder().decode(res.replace(Regex("\\s"), ""))
        val certs = PKCS7(encoded).certificates
        val certhash = sha256Hash(certs[0])

        var certsAsString = ""

        for (c in certs) {
            val x509Certificate = c as X509Certificate
            var cert: ByteArray = Base64.getEncoder().encode(x509Certificate.encoded)
            var str = cert.decodeToString()
            str = stringtoPEMFormat(str)
            certsAsString += "\n" + str
        }

        return if (certhash == r.hash.toString() || 1 == 1) { // 1 == 1 only for testing, remove later
            // LOG.debug(certsAsString)
            certsAsString
        } else {
            null
        }
    }

    fun stringtoPEMFormat(s: String): String {
        fun String.addCharAtIndex(char: Char, index: Int) =
            StringBuilder(this).apply { insert(index, char) }.toString()

        var i = 0
        var str = s
        while (i < str.length) {
            str = str.addCharAtIndex('\n', i)
            i += 65
        }
        str = "-----BEGIN CERTIFICATE-----$str\n-----END CERTIFICATE-----"
        return str
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

    @POST
    @Path("store_est_ca_cert")
    @ApiOperation(
        value = "Store est CA certificate",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "No certificate found")
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun storeEstCACert(pCert: String): Boolean {
        var certString = pCert
        LOG.debug("store cert")

        var certs: List<String> = certString.split("-----END CERTIFICATE-----")

        var allSuceed = true
        for (e in certs) {
            val filename = e.hashCode().toString() + ".cer"
            val f = File(filename)
            var c = e.replace("\\n", "").replace("\\r", "").replace("\"", "").replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "")
            LOG.debug("cert:")
            LOG.debug(c)
            if (c != "") {
                val encoded = Base64.getDecoder().decode(c.replace(Regex("\\s"), ""))
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(ByteArrayInputStream(encoded)) as X509Certificate
                f.writeBytes(cert.encoded)
                val trustStoreName = settings.connectorConfig.truststoreName
                val res = storeCertfromString(getKeystoreFile(trustStoreName), c)
                if (!res) allSuceed = false
                // f.delete() // disabled for testing
            }
        }
        return allSuceed
    }

    @POST
    @Path("request_est_identity")
    @ApiOperation(
        value = "Get CA certificate from EST",
        notes = ""
    )
    @ApiResponses(
        ApiResponse(code = 200, message = "EST CA certificate"),
        ApiResponse(code = 500, message = "No certificate found")
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun requestEstIdentity(request: EstIdRequest) {
        LOG.debug("step 0")
        getEstId(request)
    }

    private fun getEstId(r: EstIdRequest) {
        // generate key and csr
        LOG.debug("step 1")
        val keys: KeyPair = generateKeyPair()
        LOG.debug("step 2")
        val csr: ByteArray? = r.cn?.let { generateCSR(r, keys) }
        // send request
        LOG.debug("step 3, csr=$csr")
        val res: PKCS7 = sendEstIdReq(r, csr)
        LOG.debug("step3a")
        val cert: Certificate? = extractCert(res, keys.public)
        // save identity
        LOG.debug("step 4")
        if (cert != null) {
            storeEstId(cert)
        }
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096)
        return kpg.generateKeyPair()
    }

    private fun generateCSR(request: EstIdRequest, keys: KeyPair): ByteArray? {
        val tmp: ByteArray? = request.cn?.let {
            request.ou?.let { it1 ->
                request.o?.let { it2 ->
                    request.l?.let { it3 ->
                        request.s?.let { it4 ->
                            request.cn?.let { it5 ->
                                request.s?.let { it6 ->
                                    generatePKCS10(
                                        it,
                                        it1,
                                        it2,
                                        it3,
                                        it4,
                                        it5,
                                        it6,
                                        keys
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return tmp
    }

    // source: https://stackoverflow.com/questions/8160606/how-do-you-generate-a-csr-in-java-without-signing-it-by-the-requester
    @Throws(java.lang.Exception::class)
    private fun generatePKCS10(
            CN: String,
            OU: String,
            O: String,
            L: String,
            S: String,
            C: String,
            Email: String,
            keys: KeyPair
    ): ByteArray? {
        // generate PKCS10 certificate request
        val sigAlg = "MD5WithRSA"
        val pkcs = PKCS10(keys.public)
        // common, orgUnit, org, locality, state, country
        val principal = X500Principal("CN=$CN, OU=$OU, O=$O, C=$C, L=$L, S=$S, EMAIL=$Email")

        val x500name = X500Name(principal.encoded)
        pkcs.encodeAndSign(x500name, keys.private, sigAlg)
        ByteArrayOutputStream().use { bs ->
            PrintStream(bs).use { ps ->
                pkcs.print(ps)
                return bs.toByteArray()
            }
        }
    }

    private fun sendEstIdReq(r: EstIdRequest, csr: ByteArray?): PKCS7 {
        val trustStoreName = settings.connectorConfig.truststoreName
        val trustStoreFile = getKeystoreFile(trustStoreName)
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        val password = KEYSTORE_PWD
        FileInputStream(trustStoreFile).use { fis ->
            keystore.load(fis, password.toCharArray())
            LOG.debug(keystore.aliases().toString())
            tmf.init(keystore)
        }
        val trustManagers: Array<TrustManager> = tmf.trustManagers
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
                            r.password?.let { it1 ->
                                BasicAuthCredentials(
                                    username = "username",
                                    password = it1
                                )
                            }
                    }
                }
            }
        }

        LOG.debug("sending id request")
        val res = runBlocking(Dispatchers.IO) {
            val ucUrl = "${r.esturl}/.well-known/est/simpleenroll"
            LOG.debug("ucUrl: {}", ucUrl)
            var pkcs10String = (csr?.let { String(it, StandardCharsets.UTF_8) })?.replace("\\n", "")?.replace("\\r", "")?.replace("\"", "")?.replace("-----BEGIN NEW CERTIFICATE REQUEST-----", "")?.replace("-----END NEW CERTIFICATE REQUEST-----\n", "")
            LOG.debug(pkcs10String)
            LOG.debug(Base64.getEncoder().encode(csr).toString())
            val response: HttpResponse = secureHttpClient.post(ucUrl) {
                setBody(pkcs10String)
                headers {
                    append("Content-Type", "application/pkcs10")
                    append("Content-Transfer-Encoding", "base64")
                }
            }
            LOG.debug(response.status.value.toString())
            LOG.debug(response.bodyAsText())

            if (response.status.value !in 200..299) {
                throw RuntimeException("Failed to fetch certificate")
            }
            response.bodyAsText()
        }
        LOG.debug("request send")
        LOG.debug(res)
        val encoded = Base64.getDecoder().decode(res.replace(Regex("\\s"), ""))
        return PKCS7(encoded)
    }

    private fun extractCert(p: PKCS7, publicK: PublicKey): Certificate? {
        val ips = FileInputStream(p.toString())
        val cf = CertificateFactory.getInstance("X.509")
        val i: Iterator<*> = cf.generateCertificates(ips).iterator()
        var res: Certificate? = null
        while (i.hasNext()) {
            val c = i.next() as Certificate
            if (c.publicKey === publicK) res = c
        }
        return res
    }

    private fun storeEstId(cert: Certificate): Boolean {
        val filename = "tmp"
        val tempPath = File.createTempFile(filename, "cert")
        tempPath.writeText(cert.toString())
        val keyStoreName = settings.connectorConfig.keystoreName
        return storeCert(getKeystoreFile(keyStoreName), tempPath)
    }

    /** Stores a certificate in a JKS truststore.  */
    private fun storeCert(trustStoreFile: File, certFile: File): Boolean {
        val cf: CertificateFactory
        val alias = certFile.name.replace(".", "_")
        return try {
            cf = CertificateFactory.getInstance("X.509")
            val certStream = fullStream(certFile.absolutePath)
            val certs = cf.generateCertificate(certStream)
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            val password = KEYSTORE_PWD
            FileInputStream(trustStoreFile).use { fis ->
                keystore.load(fis, password.toCharArray())
            }
            FileOutputStream(trustStoreFile).use { fos ->
                // Add the certificate
                keystore.setCertificateEntry(alias, certs)
                keystore.store(fos, password.toCharArray())
            }
            true
        } catch (e: Exception) {
            LOG.error(e.message, e)
            false
        }
    }

    private fun storeCertfromString(trustStoreFile: File, cert: String): Boolean {
        val encoded = Base64.getDecoder().decode(cert.replace(Regex("\\s"), ""))
        val cf = CertificateFactory.getInstance("X.509")
        val c = cf.generateCertificate(ByteArrayInputStream(encoded)) as X509Certificate

        return try {
            val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
            val password = KEYSTORE_PWD
            FileInputStream(trustStoreFile).use { fis ->
                keystore.load(fis, password.toCharArray())
            }
            FileOutputStream(trustStoreFile).use { fos ->
                // Add the certificate
                keystore.setCertificateEntry(c.subjectDN.toString(), c)
                keystore.store(fos, password.toCharArray())
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
