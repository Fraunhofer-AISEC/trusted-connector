/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.aisec.ids.cm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamGobbler extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(StreamGobbler.class);
  InputStream is;
  OutputStream out;

  // reads everything from is until empty.
  public StreamGobbler(InputStream is, OutputStream out) {
    this.is = is;
    this.out = out;
  }

  @Override
  public void run() {
    try {
      copy(is, out);
    } catch (IOException ioe) {
      LOG.error(ioe.getMessage(), ioe);
    }
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    while (true) {
      int c = in.read();
      if (c == -1) break;
      out.write((char) c);
    }
  }

  public void close() {
    try {
      out.flush();
      out.close();
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
