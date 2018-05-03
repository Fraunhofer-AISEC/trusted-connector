/*-
 * ========================LICENSE_START=================================
 * ACME v2 client
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.dynamictls;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.osgi.service.component.annotations.Component;

import de.fhg.aisec.ids.api.acme.CertificateReloader;

/**
 * This SslContextFactory can be used instead of the default
 * org.eclipse.jetty.util.ssl.SslContextFactory by including it 
 * in jetty.xml.
 * 
 * @author Julian Schuette
 * 
 * It makes itself available as an OSGi service that allow reloading 
 * the SSL context (but nothing else). 
 *
 */
@Component(enabled=true, immediate=true, name="ids-certificate-reloader")
public class AcmeSslContextFactory extends SslContextFactory implements CertificateReloader {
	
	@Override
	public SSLEngine newSSLEngine(InetSocketAddress address) {
		// Just for debugging: log something when SSL connection is initiated
		System.out.println("AcmeSslContextFactory.newSSLEngine(address)");
		return super.newSSLEngine(address);
	}
	
	public void reloadAllCerts() {
		System.out.println("Reloading all certificates from key storey " + this.getKeyStorePath());
		
		// TODO from jetty 9.4 on, SslContextFactory provides a reload() method to refresh all SSL sessions with new certificates. It should be used after an upgrade to Karaf 4.20, instead of this poor PoC code here.
		try {
			System.out.println("Reloading keystore from " + getKeyStorePath());
			loadKeyStore(Resource.newResource(getKeyStorePath()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}