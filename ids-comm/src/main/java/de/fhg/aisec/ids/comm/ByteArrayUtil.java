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
package de.fhg.aisec.ids.comm;

public class ByteArrayUtil {

  private static final String[] lookup = new String[256];

  static {
    for (int i = 0; i < lookup.length; ++i) {
      if (i < 16) {
        lookup[i] = "0" + Integer.toHexString(i);
      } else {
        lookup[i] = Integer.toHexString(i);
      }
    }
  }

  public static String toPrintableHexString(byte[] bytes) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < bytes.length; ++i) {
      if (i > 0 && i % 16 == 0) {
        s.append('\n');
      } else {
        s.append(' ');
      }
      s.append(lookup[bytes[i] & 0xff]);
    }
    return s.toString();
  }
}
