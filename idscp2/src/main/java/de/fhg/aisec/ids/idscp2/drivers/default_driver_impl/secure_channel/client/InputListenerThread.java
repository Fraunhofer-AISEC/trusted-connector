package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

/**
 * A simple Listener thread that listens to an input stream and notifies all listeners when new data were received
 *
 * API:
 * - run() to start the listener thread
 * - register(DataAvailableListener) to register a new DataAvailableListener who will be notified when receiving data
 * - safeStop() to stop the listener thread
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class InputListenerThread extends Thread implements InputListener {
    //private static final Logger LOG = LoggerFactory.getLogger(InputListenerThread.class);

    private DataInputStream in;
    private DataAvailableListener listener = null; //no race conditions, could be empty list
    private volatile boolean running = true;

    public InputListenerThread(InputStream in){
        this.in = new DataInputStream(in);
    }

    public void run(){
        byte[] buf;
        while (running){
            try {
                int len = in.readInt();
                buf = new byte[len];
                in.readFully(buf, 0, len);
                this.listener.onMessage(buf);
            } catch (SocketTimeoutException ignore) {
                //timeout to catch safeStop() call
            } catch (EOFException e){
                listener.onClose();
                running = false; //terminate
            } catch (IOException e) {
                listener.onError();
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