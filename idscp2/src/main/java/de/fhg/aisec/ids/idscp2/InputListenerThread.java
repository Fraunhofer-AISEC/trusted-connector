package de.fhg.aisec.ids.idscp2;

import java.io.IOException;
import java.io.InputStream;
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
                //toDo interrupt read when running is set to false
                if (0 > in.read(buf,0, buf.length -1)) {
                    notifyListeners(Constants.END_OF_STREAM.getBytes());
                    running = false; //terminate
                } else {
                    notifyListeners(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
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