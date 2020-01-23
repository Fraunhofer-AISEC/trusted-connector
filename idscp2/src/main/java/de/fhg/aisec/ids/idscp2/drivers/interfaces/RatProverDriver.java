package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import de.fhg.aisec.ids.messages.IDSCPv2;

public abstract class RatProverDriver extends Thread{

    protected boolean running = true;
    protected FsmListener fsmListener;
    protected boolean successful = false;
    protected final Object resultAvailableLock = new Object();

    public void delegate(IDSCPv2.IdscpMessage message){}

    public void terminate() {
        running = false;
    }

    public void setListener(FsmListener listener){
        fsmListener = listener;
    }
}
