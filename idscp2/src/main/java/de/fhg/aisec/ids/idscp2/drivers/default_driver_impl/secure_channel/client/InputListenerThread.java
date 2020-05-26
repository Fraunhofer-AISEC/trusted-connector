package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

/**
 * A simple Listener thread that listens to an input stream and notifies a listeners
 * when new data has been received
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class InputListenerThread extends Thread implements InputListener {

    private final DataInputStream in;
    private DataAvailableListener listener = null; //no race conditions, could be empty list
    private volatile boolean running = true;

    public InputListenerThread(InputStream in) {
        this.in = new DataInputStream(in);
    }

    /*
     * Run the input listener thread that reads from wire and provides data to upper layer
     */
    public void run() {
        byte[] buf;
        while (running) {
            try {
                //first read the length
                int len = in.readInt();
                buf = new byte[len];
                //then read the data
                in.readFully(buf, 0, len);
                //provide to listener
                this.listener.onMessage(buf);
            } catch (SocketTimeoutException ignore) {
                //timeout to catch safeStop() call
            } catch (EOFException e) {
                listener.onClose();
                running = false; //terminate
            } catch (IOException e) {
                listener.onError(e);
                running = false;
            }
        }
    }

    @Override
    public void register(DataAvailableListener listener) {
        this.listener = listener;
    }


    @Override
    public void safeStop() {
        this.running = false;
    }
}