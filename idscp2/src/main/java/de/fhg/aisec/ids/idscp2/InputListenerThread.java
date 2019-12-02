package de.fhg.aisec.ids.idscp2;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * A simple Listener thread that listens to an input stream and notifies all listeners when data was received
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class InputListenerThread extends Thread implements InputListener{

    private InputStream in;
    private ArrayList<DataAvailableListener> listeners = new ArrayList<>();
    private volatile boolean running = true;

    public InputListenerThread(InputStream in){
        this.in = in;
    }

    public void run(){
        byte[] buf = new byte[2048];
        while (running){
            try {
                int len = in.read(buf, 0, buf.length - 1);
                if (0 > len) {
                    notifyListeners(Constants.END_OF_STREAM.length(), Constants.END_OF_STREAM.getBytes());
                    running = false; //terminate
                } else {
                    notifyListeners(len, buf);
                }
            } catch (SocketTimeoutException e){
                //timeout to catch safeStop() call, which allows save close and sending Client_Goodbye
                //alternative: close socket / InputStream and catch exception
                //continue;
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

    @Override
    public void unregister(DataAvailableListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(int len, byte[] bytes) {
        for (DataAvailableListener listener : listeners){
            listener.onMessage(len, bytes);
        }
    }

    @Override
    public void safeStop() {
        this.running = false;
    }
}