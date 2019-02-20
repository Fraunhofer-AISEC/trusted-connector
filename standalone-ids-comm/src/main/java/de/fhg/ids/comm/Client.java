package de.fhg.ids.comm;

import de.fhg.ids.comm.client.ClientConfiguration;
import de.fhg.ids.comm.client.IdscpClient;
import org.asynchttpclient.ws.WebSocket;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Client {

    public static void main(String[] args) throws Exception {
        // Configure and start client (blocks until IDSCP has finished)
        IdscpClient client = new IdscpClient().config(
                new ClientConfiguration.Builder()
                        .setSha256CertificateHashes(Arrays.asList(
                                DatatypeConverter.parseHexBinary(
                                        "A3374C859BD5E819B5B50486040FB2DCEA5EC4E3F04DDBC3D059E162B8B7AB37"),
                                DatatypeConverter.parseHexBinary(
                                        "4439DA49F320E3786319A5CF8D69F3A0831C4801B5CE3A14570EA84E0ECD82B0")))
                        .build());
        WebSocket wsClient = client.connect("consumer-core", 9292);

        try (ServerSocket listener = new ServerSocket(4441)) {
            System.out.println("Waiting for data connections on port 4441...");
            try (Socket socket = listener.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while(true) {
                    if (in.ready()) {
                        String msg = in.readLine();
                        if (msg.equals("exit")) {
                            System.out.println("Exiting...");
                            System.exit(0);
                        }
                        wsClient.sendTextFrame(msg);
                    }
                }
            }
        }
    }

}
