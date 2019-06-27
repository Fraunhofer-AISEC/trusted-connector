package de.fhg.aisec.ids.comm.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketHandler extends Thread {

    private final Socket socket;

    SocketHandler(Socket socket) {
        this.socket = socket;
        this.setDaemon(true);
        this.start();
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while(true) {
                String msg = in.readLine();
                if (msg == null) {
                    System.out.println("Last line was null, closing socket...");
                    socket.close();
                    break;
                }
                System.out.println("Forwarding message: " + msg);
                Client.putMessage(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
