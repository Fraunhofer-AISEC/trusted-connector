/*-
 * ========================LICENSE_START=================================
 * ids-settings
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
package de.fhg.aisec.ids.settings

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapdb.elsa.ElsaMaker
import org.mapdb.elsa.ElsaSerializerPojo

class ElsaSerializer<T : Any> : Serializer<T> {
    private val serializer: ElsaSerializerPojo = ElsaMaker().make()

    override fun serialize(
        output: DataOutput2,
        obj: T
    ) {
        serializer.serialize(output, obj)
    }

    override fun deserialize(
        input: DataInput2,
        available: Int
    ): T {
        return serializer.deserialize(input)
    }
}
