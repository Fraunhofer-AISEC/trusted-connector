/*-
 * ========================LICENSE_START=================================
 * rat-repository
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
package de.fhg.ids.attestation;
import javax.ws.rs.core.MediaType;

public class MediaTypeExt extends MediaType {
    /**
     * A {@code String} constant representing "{@value #APPLICATION_PROTOBUF}" media type.
     */
    public final static String APPLICATION_PROTOBUF = "application/x-protobuf";
    /**
     * A {@link javax.ws.rs.core.MediaType} constant representing "{@value #APPLICATION_PROTOBUF}" media type.
     */
    public final static MediaType APPLICATION_PROTOBUF_TYPE = new MediaType("application", "x-protobuf");
}
