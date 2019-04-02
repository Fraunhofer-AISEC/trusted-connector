package de.fhg.ids.comm;

import de.fhg.ids.comm.client.ClientConfiguration;
import de.fhg.ids.comm.client.IdscpClient;
import org.asynchttpclient.ws.WebSocket;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.min;

public class Client {

    private final static long MAX_BACKOFF_MS = 300000;

    private static BlockingQueue<String> queue = new LinkedBlockingQueue<>(1000);

    static void putMessage(String message) throws InterruptedException {
        queue.put(message);
    }

    public static void main(String[] args) throws Exception {
        // Configure and start client (blocks until IDSCP has finished)
        IdscpClient client = new IdscpClient().config(
                new ClientConfiguration.Builder()
                        .setSha256CertificateHashes(Arrays.asList(
                                // test localhost certificate
                                DatatypeConverter.parseHexBinary(
                                        "A3374C859BD5E819B5B50486040FB2DCEA5EC4E3F04DDBC3D059E162B8B7AB37"),
                                // example consumer-core certificate
                                DatatypeConverter.parseHexBinary(
                                        "4439DA49F320E3786319A5CF8D69F3A0831C4801B5CE3A14570EA84E0ECD82B0")))
                        .build());

        Thread listenerThread = new Thread(() -> {
            try (ServerSocket listener = new ServerSocket(4441)) {
                System.out.println("Waiting for data connection on port 4441...");
                while (true) {
                    try {
                        Socket socket = listener.accept();
                        new SocketHandler(socket);
                    } catch (IOException e) {
                        System.err.println("Error creating SocketHandler");
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error while trying listening on port 4441");
                e.printStackTrace();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();

        long backoff = 1000;

        System.out.println("Connecting to IDS-backend...");
        WebSocket wsClient = client.connect("consumer-core", 9292);
        System.out.println("Connected.");

        // Send messages
        String message = queue.take();
        while (true) {
            if (message.equals("exit")) {
                System.out.println("Read exit message, exiting...");
                break;
            }
            System.out.println("Sending:\n" + message);
            if(wsClient.sendTextFrame(message).await().isSuccess()) {
                // Get next message from queue
                message = queue.take();
                backoff = 1000;
            } else {
                System.err.println("Send not successful, reconnect and try to send message again in " + backoff + " ms");
                Thread.sleep(backoff);
                // Exponentially increase backoff time
                backoff = min(backoff * 2, MAX_BACKOFF_MS);
                try {
                    wsClient = client.connect("consumer-core", 9292);
                } catch (Exception ce) {
                    ce.printStackTrace();
                }
            }
        }

    }

}
