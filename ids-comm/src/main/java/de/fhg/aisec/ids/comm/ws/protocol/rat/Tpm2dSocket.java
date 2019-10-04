/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.comm.ws.protocol.rat;

import de.fhg.aisec.ids.messages.AttestationProtos.RemoteToTpm2d;
import de.fhg.aisec.ids.messages.AttestationProtos.Tpm2dToRemote;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Tpm2dSocket extends Socket {

  private final DataInputStream is;
  private final DataOutputStream os;

  public Tpm2dSocket(String host) throws IOException {
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
