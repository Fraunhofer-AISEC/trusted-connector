package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d;

import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.RemoteToTpm2d;
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.Tpm2dToRemote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A TPM2d Socket for communication with the Trusted Platform Module
 */
public class TPM2dSocket extends Socket {

    private final DataInputStream is;
    private final DataOutputStream os;

    public TPM2dSocket(String host) throws IOException {
        super(host, 9505);
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
