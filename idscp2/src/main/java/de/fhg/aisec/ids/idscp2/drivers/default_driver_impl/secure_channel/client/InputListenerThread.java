package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TlsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

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
    private ArrayList<DataAvailableListener> listeners = new ArrayList<>(); //no race conditions, could be empty list
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
                notifyListeners(buf);

            } catch (SocketTimeoutException e) {
                //timeout to catch safeStop() call, which allows save close and sending Client_Goodbye
                //alternative: close socket / InputStream and catch exception
                //continue;
                //toDo offset when timeout while reading ???
            } catch (EOFException e){
                notifyListeners(TlsConstants.END_OF_STREAM.getBytes());
                running = false; //terminate
            } catch (IOException e) {
                //e.printStackTrace();
                running = false;
            }
        }
        listeners.clear();
    }

    @Override
    public void register(DataAvailableListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(byte[] bytes) {
        for (DataAvailableListener listener : listeners){
            listener.onMessage(bytes);
        }
    }

    @Override
    public void safeStop() {
        this.running = false;
    }
}