package de.fhg.aisec.ids.camel.processors.multipart

import com.google.common.collect.MapMaker
import de.fhg.aisec.ids.idscp2.daps.aisecdaps.AisecDapsDriver.Companion.toHexString
import org.apache.camel.component.http.HttpClientConfigurer
import org.apache.http.HttpResponse
import org.apache.http.conn.ManagedHttpClientConnection
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpCoreContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.cert.Certificate

@Component("certExposingHttpClientConfigurer")
class CertExposingHttpClientConfigurer : HttpClientConfigurer {
    override fun configureHttpClient(clientBuilder: HttpClientBuilder) {
        clientBuilder.addInterceptorLast { response: HttpResponse, context ->
            val routedConnection = context.getAttribute(HttpCoreContext.HTTP_CONNECTION) as ManagedHttpClientConnection
            routedConnection.sslSession?.let { sslSession ->
                val certs = sslSession.peerCertificates
                val certHash = MessageDigest.getInstance("SHA-256").digest(certs[0].encoded).toHexString().lowercase()
                certificateMap += certHash to certs
                response.addHeader(SERVER_CERTIFICATE_HASH_HEADER, certHash)
                if (LOG.isDebugEnabled) {
                    LOG.debug("Captured server certificate with SHA256 fingerprint $certHash.")
                }
            }
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(CertExposingHttpClientConfigurer::class.java)
        const val SERVER_CERTIFICATE_HASH_HEADER = "ServerCertificateHash"
        val certificateMap: MutableMap<String, Array<Certificate>> = MapMaker().weakKeys().makeMap()
    }
}
