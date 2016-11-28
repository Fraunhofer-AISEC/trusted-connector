package de.fhg.ids.comm.ws.protocol.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;

public class ErrorHandler {
	
	private Logger LOG = LoggerFactory.getLogger(ProtocolMachine.class);

	public boolean handleError(Event e, ProtocolState state, boolean isConsumer) {
		String entity = "Provider";
		if(isConsumer) {
			entity = "Consumer";
		}
		LOG.debug("*******************************************************************************************************");
		LOG.debug("*  error during rat protocol execution ");
		LOG.debug("*  -> state: " + state.description());
		LOG.debug("*  -> side: "+entity+"");		
		LOG.debug("*  -> error: " + e.getMessage().getError().getErrorMessage());
		LOG.debug("*******************************************************************************************************");
		/*
		 * if(errorWasHandled) {
		 *     return true; // and thus change the state to IDSCP_START
		 * }
		 * else {
		 */
			return false;
		/*
		 * }
		 */
	}

}
