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
package de.fhg.aisec.ids.comm.ws.protocol.error;

import de.fhg.aisec.ids.comm.ws.protocol.ProtocolState;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class ErrorHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

  public boolean handleError(Event e, ProtocolState state, boolean isConsumer) {
    String entity = isConsumer ? "Consumer" : "Provider";

    LOG.debug(
        "**************************************************************************************************");
    LOG.debug("*  error handler during rat protocol execution ");
    LOG.debug("*  -> state: " + state.description());
    LOG.debug("*  -> side: " + entity + "");
    LOG.debug("*  -> error: " + e.getMessage().getError().getErrorMessage());
    LOG.debug(
        "**************************************************************************************************");

    return true;
  }
}
