package de.fhg.aisec.ids.settings

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapdb.elsa.ElsaMaker
import org.mapdb.elsa.ElsaSerializer
import org.mapdb.elsa.ElsaSerializerBase
import org.mapdb.elsa.ElsaSerializerPojo

class OsgiElsaSerializer<T> : Serializer<T> {
    private val serializer: ElsaSerializerPojo

    init {
        // We must temporarily fix the context class loader when creating the serializer,
        // or else we get ClassNotFoundExceptions later on
        val currentThread = Thread.currentThread()
        val cl = currentThread.contextClassLoader
        currentThread.contextClassLoader = this.javaClass.classLoader
        serializer = ElsaMaker().make()
        currentThread.contextClassLoader = cl
    }

    override fun serialize(output: DataOutput2, obj: T) {
        serializer.serialize(output, obj)
    }

    override fun deserialize(input: DataInput2, available: Int): T {
        return serializer.deserialize<T>(input)
    }
}