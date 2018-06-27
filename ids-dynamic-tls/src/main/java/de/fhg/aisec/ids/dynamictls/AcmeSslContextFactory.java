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

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * This SslContextFactory registers started instances to an OSGi service
 * that allows reloading of all active SslContextFactory instances.
 *
 * @author Michael Lux
 */
public class AcmeSslContextFactory extends SslContextFactory {
	private static final Logger LOG = LoggerFactory.getLogger(AcmeSslContextFactory.class);

	@Override
	protected void doStart() throws Exception {
		LOG.debug("Start " + this.toString());
		JettySslContextFactoryReloader.addFactory(this);
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		LOG.debug("Stop " + this.toString());
		JettySslContextFactoryReloader.removeFactory(this);
		super.doStop();
	}

	@Override
	public void reload(Consumer<SslContextFactory> consumer) throws Exception {
		LOG.info("Reload " + this.toString());
		super.reload(consumer);
	}

	@Override
    public String toString() {
        return String.format("%s@%x (%s)", this.getClass().getSimpleName(), this.hashCode(), this.getKeyStorePath());
    }

}