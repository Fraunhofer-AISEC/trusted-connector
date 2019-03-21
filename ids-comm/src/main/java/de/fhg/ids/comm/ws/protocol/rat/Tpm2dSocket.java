package de.fhg.ids.comm.ws.protocol.rat;

import de.fhg.aisec.ids.messages.AttestationProtos.RemoteToTpm2d;
import de.fhg.aisec.ids.messages.AttestationProtos.Tpm2dToRemote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Tpm2dSocket extends Socket {

    private final DataInputStream is;
    private final DataOutputStream os;

    public Tpm2dSocket() throws IOException {
        super("localhost", 9505);
        is = new DataInputStream(this.getInputStream());
        os = new DataOutputStream(this.getOutputStream());
    }

    public Tpm2dToRemote requestAttestation(RemoteToTpm2d request) throws IOException {
        // Write attestation request message
        byte[] requestBytes = request.toByteArray();
        os.writeInt(requestBytes.length);
        os.write(requestBytes);
        // Read attestation result message
        byte[] resultBytes = new byte[is.readInt()];
        is.readFully(resultBytes);
        return Tpm2dToRemote.parseFrom(resultBytes);
    }

}
