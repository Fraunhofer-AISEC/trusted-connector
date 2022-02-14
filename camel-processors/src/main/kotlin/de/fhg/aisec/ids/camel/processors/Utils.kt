/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids.camel.processors

import de.fraunhofer.iais.eis.ids.jsonld.Serializer
import java.net.URI
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar

object Utils {
    val SERIALIZER: Serializer by lazy { Serializer() }
    val TYPE_DATETIMESTAMP: URI = URI.create("http://www.w3.org/2001/XMLSchema#dateTimeStamp")
    fun newDuration(millis: Long): Duration = DatatypeFactory.newInstance().newDuration(millis)
}

fun XMLGregorianCalendar.copy() = this.clone() as XMLGregorianCalendar
