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
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.UUID
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
    fun requestEstCert(request: EstCaCertRequest): String {
        return getEstCaCert(request)
    }

    private fun getEstCaCert(r: EstCaCertRequest): String {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(r.url.toString() + "/.well-known/est/cacerts"))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        LOG.debug(response.body())

        // val cert = response.body();
        // for testing
        val cert = "MIIJJAYJKoZIhvcNAQcCoIIJFTCCCRECAQExADALBgkqhkiG9w0BBwGgggj3MIID9jCCAd6gAwIB" +
            "AgIBATANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDDAtJRFMgUm9vdCBDQTAeFw0yMjA5MjkwODUy" +
            "MDhaFw0yMjEwMjkwODUyMDhaMCMxITAfBgNVBAMMGElEUyBJZGVudGl0eSBTZXJ2aWNlcyBDQTCC" +
            "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM2A4Ob81BXgoZLsnKJ0Rp/QQES+Xd9dTVab" +
            "Cp3Mcvhktf2v6BIZK1xBNwPExUUuC8EIxubFuBNUF06PDgSa7v9K//dLpOlTy6n7dBbSJgnEnYb+" +
            "6NkFDPOytYyVY6jmoO5jWzkoHMcKpnXep07yJw/W4lUkVrjijQQNN/+pT7XdrdB2pIaSmHBxDWMk" +
            "XSG+s1kHW/odilovkJAlIzOSV2x9M5RRxPnSl/k9G828ZlViG0SZxS4FPKCa6zsU7qLS24hGesXP" +
            "TW2S9rD8UTzuwrZ9ZzaayJaa5Z7CqnFBMP5u1Hl/PUuX/3tJqEUU10xBx1YAqiTFxoq+TCwcdN9T" +
            "Zq8CAwEAAaNCMEAwEgYDVR0TAQH/BAgwBgEB/wIBADALBgNVHQ8EBAMCAcYwHQYDVR0OBBYEFDkH" +
            "Wv5ti2p4ZbSCaijPEhnUzfK+MA0GCSqGSIb3DQEBCwUAA4ICAQCZTdrHqVQ87XhX5retAYrynHtM" +
            "FHMjLgl7PYQ3DK8RkANws1oW9q5LA+WXOi3X2MqEn/ocilaiSuYBV4DrBO4c+yixvcqU64YNj6LC" +
            "zm97msNTnU5AXXV4izFpdALp9pFyo9UnCxB2i9lBELXzidQ/hPjvr5+R3mBozJrwgOBINU96kg/6" +
            "DOa+vRQkEs/dbjzY9ZzkpRpTxZPT3HLvxx8+hhaxUQzanJQ65BWm7kPv9MfLLcA7cd3GTtdJAnrQ" +
            "v11qYE2NT4p6fIkVpKPd9lCbRpMLBj0pH740AH0wcmThL8YkxMw18XzDeNh8UOqAi52M9xqfjaAC" +
            "5QI2AU2wYwmZQnGWEHTqmGGfyB7a/JBm28Mew56OXrZVNxpRa0CShIAXVCV+pu++FXFcuHutxg6h" +
            "tuQFqhBoUUJU2/KQ+6qeXLPgl4klQlnqASYYD/2HAvr0aVQFHubK5EhRop3zcyMqpbbaO1WHtOQZ" +
            "IZnYBpTL1jSkEqcHPSi4WnmnNMRugRvgrpYxuFqdA6gaaFesHX6YCs036pfeDEbeuUFkasaxzu1F" +
            "sVwwEB9IN2phZ5v4Jq6b8j3qDRNy0S8ELqr4hQJMkT+ynzSq88+bsCMRBnv/M/TGXJraNHFfnMI3" +
            "Kpzc6ro3Ukp2ZoVb1pySW+s9rfjdUKPd0OMiHtBt1XIh4ri3yTCCBPkwggLhoAMCAQICFCRSmypU" +
            "AMoHrD8f44aMr7YMipgyMA0GCSqGSIb3DQEBCwUAMBYxFDASBgNVBAMMC0lEUyBSb290IENBMB4X" +
            "DTIyMDkyOTA4NTIwOFoXDTIyMTAyOTA4NTIwOFowFjEUMBIGA1UEAwwLSURTIFJvb3QgQ0EwggIi" +
            "MA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCnArmrL1umoNEyBT36p5yA2WC1tmkmbNUsfYAD" +
            "aUYVV8zxIR9d2B6iT8Ydga+dQxzcZflN5xnLIPbUVy3KoqxgrpmMu41JqtZ112Hl76Wt+pgBO4Xk" +
            "HgxeHRRoNQKyDK0y08m1Y8mgrnZM//o9WW3ekZUswbx4/FwirgO3JY+P+uxKHmjiiDay4Wh3spsM" +
            "SgvBaRxcmAKtnUn1t1H28/XU40aP5xYrZnFKJgEoYTXw90XZP1fvYH+DwCYJ6N60QJai1MhnAzkH" +
            "ByS9o51NQNN4CALG8s7Qqu9KiykfNW30rf2e6yOdwGobo/9+YVUkJowqTKw8HHghgNoUuxPmMT4W" +
            "VWxm8kYS0ZTcCJ43m9bvaKtk/fVTetrSUbNPXF9090hir3VMZPvIi+H2nMMVenDE8+nyRq1NBgbP" +
            "9/ItOnTX1Oe7ZbNzACMga042L0SQHZ8wH61WM6dO/cJdhIFEyD9S6GILrNR/reL+2XVOtUjDlaVV" +
            "BH1odGt9BaYnMgzxG/D4cluI+YtGbQE+ny5iFm2Ybaqhqi0lM9SUi17wvr4ifLwCi33CCAvZ8C29" +
            "RIc2I2C/p3f5PiKRok8AXOLp85rdUKhXAO3VqUX+QY9GTX2bKTidjNOonT76yCVauxn/HjojoTAI" +
            "vcgfZgar0XbGnWb1tSUzQ1Tz/7fAXQYxkCzeAwIDAQABoz8wPTAPBgNVHRMBAf8EBTADAQH/MAsG" +
            "A1UdDwQEAwIBxjAdBgNVHQ4EFgQUEzx1MpY+XdSv+OB6+ik64KHDikMwDQYJKoZIhvcNAQELBQAD" +
            "ggIBAIjGXDujDoDbgZxzNJZCh5dV/s8KDc4FDvKrpn+XUdtJSn/uqtr6/69vVuv5KqaGigWIwCdS" +
            "wdVxN2sGZP9Dp8k7MyEj0Pw/dd/GSB9YphsoJ39yf/PX1mfnp2wdmfJ9ph+EWB0W+FLF6KuxtcM3" +
            "SNqytthGtJohcMUGGcAIErm8W69yqvY6YELornlBQyFgTOMSPYNN3W2GeC8YGHbezvaaCpTFsgp8" +
            "NjvHL+FCVTyvOUlgEbxo5zPJ3d0sI7OaQBjMbqtZZPb6X+gVM6y6D1W50S6U9nG7c2MuTNdTkpqP" +
            "CdvOojgSa+zxnBO0GsLIiQqVCzNp7P+dfyyjRlgG9V8dq3ftlRneVAe8lBph19daQDTeLIIBzN/F" +
            "qNpYp8iVk+PRunOJI97ov1fKaapyi6h4n3CHooFGhbrlINFM25JL3SI2BmhslQ3WK3Npk1Fxa3K1" +
            "39Q/M/Vqb2jD/e+XQ2k0zdBHVpBsLfLUjU4YmXeb7xBi/11gCrX7Ntt5UiY9J8CBxkXWxFocHXUv" +
            "v7Nc4E6OwtQ0OlInMEw3qU8fsPWYdamvsPPtfNz8T6iEfZAS3TaA41CZ5noZ/NK8TB3PnZe6hlkW" +
            "djrB/++BETnVQAfjS6wIKJz9R8EGy1Yoq3sujY6dPA3bO03191Pl78RmQlgUy4i9jS1CZekzqtgo" +
            "R48CoQAxAA=="

        // verify hash
        // val certhash = sha256Hash(cert)
        // if (certhash == r.hash.toString()) {
        return cert
    }

    /**
     * Convert byte to hexadecimal chars without any dependencies to libraries.
     * @param num Byte to get hexadecimal representation for
     * @return The hexadecimal representation of the given byte value
     */
    private fun byteToHex(num: Int): CharArray {
        val hexDigits = CharArray(2)
        hexDigits[0] = Character.forDigit(num shr 4 and 0xF, 16)
        hexDigits[1] = Character.forDigit(num and 0xF, 16)
        return hexDigits
    }

    private val hexLookup = HashMap<Byte, CharArray>()

    /**
     * Encode a byte array to a hex string
     * @param byteArray Byte array to get hexadecimal representation for
     * @return Hexadecimal representation of the given bytes
     */
    private fun encodeHexString(byteArray: ByteArray): String {
        return byteArray.map { hexLookup.computeIfAbsent(it) { num: Byte -> byteToHex(num.toInt()) } }
            .joinToString { "" }
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
    fun storeEstCACert(cert: String): Boolean {
        val filename = "tmp"
        val f = File(filename)
        f.writeText(cert)
        val trustStoreName = settings.connectorConfig.truststoreName
        val res = storeCert(getKeystoreFile(trustStoreName), f.absoluteFile)
        f.delete()
        return res
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
        getEstId(request)
    }

    private fun getEstId(r: EstIdRequest) {
        // generate key and csr
        LOG.debug("step 1")
        val keys: KeyPair = generateKeyPair()
        LOG.debug("step 2")
        val csr: ByteArray? = r.id?.let { generateCSR(it, keys) }
        // send request
        LOG.debug("step 3, csr=$csr")
        val cert: Certificate? = sendEstIdReq(r, csr)
        // save identity
        LOG.debug("step 4")
        if (cert != null) {
            storeEstId(cert)
        }
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096)
        val kp = kpg.generateKeyPair()
        return kp
    }

    private fun generateCSR(request: Identity, keys: KeyPair): ByteArray? {
        val tmp: ByteArray? = request.cn?.let {
            request.ou?.let { it1 ->
                request.o?.let { it2 ->
                    request.l?.let { it3 ->
                        request.s?.let { it4 ->
                            request.c?.let { it5 ->
                                request.email?.let { it6 ->
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
        val principal = X500Principal("CN=$CN, OU=$OU, O=$O, C=$C, EMAIL=$Email")

        val x500name = X500Name(principal.encoded)
        pkcs.encodeAndSign(x500name, keys.private, sigAlg)
        ByteArrayOutputStream().use { bs ->
            PrintStream(bs).use { ps ->
                pkcs.print(ps)
                return bs.toByteArray()
            }
        }
    }

    private fun sendEstIdReq(r: EstIdRequest, csr: ByteArray?): Certificate? {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .header("Content-Type", "application/pkcs10")
            .uri(URI.create(r.esturl.toString() + "/.well-known/est/simplerenroll"))
            .POST(HttpRequest.BodyPublishers.ofByteArray(csr))
            .build()
        LOG.debug("here")
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val cert: Certificate? = null
        LOG.debug(response.body())
        return cert
    }

    private fun storeEstId(cert: Certificate): Boolean {
        val filename = "tmp"
        val tempPath = File.createTempFile(filename, "cert")
        File("tmp.cert").writeText(cert.toString())
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
