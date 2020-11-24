package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.client

import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException

/**
 * A simple Listener thread that listens to an input stream and notifies a listeners
 * when new data has been received
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class InputListenerThread(`in`: InputStream) : Thread(), InputListener {
    private val `in`: DataInputStream = DataInputStream(`in`)
    private var listener: DataAvailableListener? = null //no race conditions, could be empty list

    @Volatile
    private var running = true

    /*
     * Run the input listener thread that reads from wire and provides data to upper layer
     */
    override fun run() {
        var buf: ByteArray
        while (running) {
            try {
                //first read the length
                val len = `in`.readInt()
                buf = ByteArray(len)
                //then read the data
                `in`.readFully(buf, 0, len)
                //provide to listener
                listener!!.onMessage(buf)
            } catch (ignore: SocketTimeoutException) {
                //timeout to catch safeStop() call
            } catch (e: EOFException) {
                listener!!.onClose()
                running = false
            } catch (e: IOException) {
                listener!!.onError(e)
                running = false
            }
        }
    }

    override fun register(listener: DataAvailableListener) {
        this.listener = listener
    }

    override fun safeStop() {
        running = false
    }

}