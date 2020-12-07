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
class InputListenerThread(inputStream: InputStream, private var listener: DataAvailableListener) : Thread() {
    private val dataInputStream: DataInputStream = DataInputStream(inputStream)

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
                val len = dataInputStream.readInt()
                buf = ByteArray(len)
                //then read the data
                dataInputStream.readFully(buf, 0, len)
                //provide to listener
                listener.onMessage(buf)
            } catch (ignore: SocketTimeoutException) {
                //timeout to catch safeStop() call
            } catch (e: EOFException) {
                listener.onClose()
                running = false
            } catch (e: IOException) {
                listener.onError(e)
                running = false
            }
        }
    }

    fun safeStop() {
        running = false
    }

}