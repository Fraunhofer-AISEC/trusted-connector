/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.ids.comm.ws.protocol.rat;

import java.security.SecureRandom;

public final class NonceGenerator {
  private static final SecureRandom sr = new SecureRandom();

  private NonceGenerator() {}

  /**
   * Generate a crypto-secure random hex String of length numChars
   *
   * @param numBytes Desired String length
   * @return The generated crypto-secure random hex String
   */
  public static byte[] generate(int numBytes) {
    byte[] randBytes = new byte[numBytes];
    sr.nextBytes(randBytes);
    return randBytes;
  }
}
