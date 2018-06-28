/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.client;

import java.net.URI;

import org.apache.camel.component.ahc.AhcComponent;
import org.apache.camel.component.ahc.AhcEndpoint;

/**
 */
public class WsComponent extends AhcComponent {


	@Override
    protected String createAddressUri(String uri, String remaining) {
    	if (uri.startsWith("idsclientplain:")) {
    		return uri.replaceFirst("idsclientplain:", "ws:");
    	} else if (uri.startsWith("idsclient:")) {
    		return uri.replaceFirst("idsclient:", "wss:");    		
    	}
    	//Should not happen
    	return uri;
    }

    @Override
    protected AhcEndpoint createAhcEndpoint(String endpointUri, AhcComponent component, URI httpUri) {
        return new WsEndpoint(endpointUri, (WsComponent)component);
    }
}
