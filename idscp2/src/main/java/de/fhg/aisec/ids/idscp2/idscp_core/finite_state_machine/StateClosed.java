package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;


import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM.FSM_STATE;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Condition;


class StateClosed extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateClosed.class);

    public StateClosed(FSM fsm,
                       DapsDriver dapsDriver,
                       Condition onMessageLock,
                       String[] localSupportedRatSuite,
                       String[] localExpectedRatSuite){


        /*---------------------------------------------------
         * STATE_CLOSED - Transition Description
         * ---------------------------------------------------
         * onICM: start_handshake --> {send IDSCP_HELLO, set handshake_timeout} --> STATE_WAIT_FOR_HELLO
         * ALL_OTHER_MESSAGES ---> STATE_CLOSED
         * --------------------------------------------------- */
        this.addTransition(InternalControlMessage.START_IDSCP_HANDSHAKE.getValue(), new Transition(
                event -> {

                    LOG.debug("Get DAT Token vom DAT_DRIVER");
                    byte[] dat = dapsDriver.getToken();

                    LOG.debug("Send IDSCP_HELLO");
                    IDSCPv2.IdscpMessage idscpHello = IdscpMessageFactory.
                            getIdscpHelloMessage(dat, localSupportedRatSuite, localExpectedRatSuite);

                    if (!fsm.sendFromFSM(idscpHello)) {
                      LOG.error("Cannot send IdscpHello. Close connection");
                      runEntryCode(fsm);
                      onMessageLock.notifyAll();
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    runExitCode(onMessageLock);
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_HELLO);
                }
        ));

        this.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_CLOSED");
                    return this;
                }
        );

    }

    private void runExitCode(Condition onMessageLock){
        //State Closed exit code
        onMessageLock.signalAll(); //enables fsm.onMessage()
    }

    @Override
    void runEntryCode(FSM fsm){
        //State Closed entry code
        LOG.debug("Switched to state STATE_CLOSED");
        LOG.debug("Terminate and free all resources and lock fsm forever");
        fsm.lockFsm();
    }

}
