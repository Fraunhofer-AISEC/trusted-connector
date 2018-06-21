/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole.deserializer;

import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.Serializers;

import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.PlainLiteral;

public class CustomModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public CustomModule() {
        super("CustomModule");
        this.addDeserializer(PlainLiteral.class, new PlainLiteralDeserializer());
        this.addDeserializer(SecurityProfile.class, new SecurityProfileDeserializer());
    }

/*
private Serializers createSerializers() {
	SimpleSerializers serializers = new SimpleSerializers();
	//serializers.addSerializer(zipCodeSerializer());
	return serializers ;
}

private Deserializers createDeserializers() {
	SimpleDeserializers deserializers = new SimpleDeserializers();
	deserializers.addDeserializer(PlainLiteral.class, new PlainLiteralDeserializer());
	deserializers.addDeserializer(SecurityProfile.class, new SecurityProfileDeserializer());
	return deserializers ;
}

private PlainLiteralDeserializer plainLiteralDeserializer() {
	return new PlainLiteralDeserializer(PlainLiteral.class, value -> new ZipCode(value));
}

private ValueObjectSerializer zipCodeSerializer() {
	return new ValueObjectSerializer<>(ZipCode.class, value -> value.getCode());
}*/

}
