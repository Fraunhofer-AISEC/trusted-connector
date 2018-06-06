package de.fhg.aisec.ids.settings

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapdb.elsa.ElsaMaker
import org.mapdb.elsa.ElsaSerializer

class OsgiElsaSerializer : Serializer<Any> {
    private val serializer: ElsaSerializer

    init {
        // We must temporarily fix the context class loader when creating the serializer,
        // or else we get ClassNotFoundExceptions later on
        val currentThread = Thread.currentThread()
        val cl = currentThread.contextClassLoader
        currentThread.contextClassLoader = this.javaClass.classLoader
        serializer = ElsaMaker().make()
        currentThread.contextClassLoader = cl
    }

    override fun serialize(output: DataOutput2, obj: Any) {
        serializer.serialize(output, obj)
    }

    override fun deserialize(input: DataInput2, available: Int): Any {
        return serializer.deserialize<Any>(input)
    }
}