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
package de.fhg.aisec.ids.comm.ws.protocol.fsm;

import de.fhg.aisec.ids.api.conm.RatResult;
import de.fhg.aisec.ids.comm.ws.protocol.ProtocolState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * Implementation of a finite state machine (FSM).
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class FSM {

  protected String currentState = null;
  protected Map<String, State> states;
  protected HashSet<ChangeListener> successChangeListeners;
  protected HashSet<ChangeListener> failChangeListeners;
  private String initialState = null;
  private RatResult ratResult;
  private String metaData;
  private String dynamicAttributeToken;

  public FSM() {
    this.states = new HashMap<>();
    this.successChangeListeners = new HashSet<>();
    this.failChangeListeners = new HashSet<>();
  }

  public String getState() {
    return currentState;
  }

  public void addState(ProtocolState state) {
    addState(state.id(), null, null, null);
  }

  /**
   * Defines an additional state. A state is identified by a name and has three blocks of code
   * assigned to it:
   *
   * <p>1) entryCode is executed when the state is entered from another state (after the
   * transition's code has been executed)
   *
   * <p>2) exitCode is executed when the state is left for another state (before the transition's
   * code is executed)
   *
   * <p>3) alwaysRunCode is always executed when the FSM enters this state (even if it has been in
   * that state before)
   *
   * <p>All three Runnables may be null. Their result has no impact on the FSM's state, Exceptions
   * thrown are ignored.
   *
   * @param state
   * @param entryCode
   * @param exitCode
   * @param alwaysRunCode
   */
  public void addState(
      String state, Runnable entryCode, Runnable exitCode, Runnable alwaysRunCode) {
    if (states.size() == 0) {
      this.initialState = state;
      this.currentState = state;
    }
    if (!states.containsKey(state)) {
      states.put(state, new State(entryCode, exitCode, alwaysRunCode));
    } else {
      throw new NoSuchElementException("Missing state: " + state);
    }
  }

  /**
   * Defines initial state of this FSM. If no initial state is defined explicitly, the first added
   * state is the initial state.
   *
   * @param state The initial state of this FSM
   */
  public void setInitialState(ProtocolState state) {
    if (!this.states.containsKey(state.id())) {
      throw new NoSuchElementException("Missing state: " + state.id());
    }
    this.initialState = state.id();
  }

  /** Resets FSM to it initial state */
  public void reset() {
    this.currentState = this.initialState;
  }

  private void setState(String state) {
    boolean runExtraCode = !state.equals(currentState);
    if (runExtraCode && currentState != null) {
      states.get(currentState).runExitCode();
    }
    currentState = state;
    states.get(currentState).runAlwaysCode();
    if (runExtraCode) {
      states.get(currentState).runEntryCode();
    }

    // If event-less transition is defined for current node, trigger it immediately
    if (states.get(currentState).transitions.containsKey(null)) {
      feedEvent(null);
    }
  }

  /**
   * Add a new transition to this FSM.
   *
   * @param trans The transition to be added to this FSM
   */
  public void addTransition(Transition trans) {
    State st = states.get(trans.startState);
    if (st == null) {
      throw new NoSuchElementException("Missing start state: " + trans.startState);
    }
    if (!states.containsKey(trans.endState)) {
      throw new NoSuchElementException("Missing end state: " + trans.endState);
    }
    st.addTransition(trans);
  }

  public void addSuccessChangeListener(ChangeListener cl) {
    successChangeListeners.add(cl);
  }

  public void addFailChangeListener(ChangeListener cl) {
    failChangeListeners.add(cl);
  }

  public void feedEvent(Event event) {
    Object evtKey = event.getKey();
    State state = states.get(currentState);
    Transition trans = state.transitions.get(evtKey);
    if (trans != null) {
      if (trans.doBeforeTransition(event)) {
        setState(trans.endState);
        successChangeListeners.forEach(l -> l.stateChanged(this, event));
      } else {
        failChangeListeners.forEach(l -> l.stateChanged(this, event));
      }
    }
  }

  public String toDot() {
    StringBuilder sb = new StringBuilder();
    sb.append("digraph finite_state_machine {\n");
    sb.append("	rankdir=LR;\n");
    sb.append("	node [shape = ellipse];\n");
    for (Entry<String, State> from : states.entrySet()) {
      for (Object t : from.getValue().transitions.keySet()) {
        String to = from.getValue().transitions.get(t).endState;
        Object eventKey = from.getValue().transitions.get(t).event;
        sb.append(
            "    "
                + from.getKey().replace(':', '_')
                + " -> "
                + to.replace(':', '_')
                + " [ label=\""
                + eventKey
                + "\" ];\n");
      }
    }
    sb.append("			}");
    return sb.toString();
  }

  public void handleRatResult(RatResult attestationResult) {
    this.ratResult = attestationResult;
  }

  public RatResult getRatResult() {
    return ratResult;
  }

  public void setMetaData(String metaData) {
    this.metaData = metaData;
  }

  public String getMetaData() {
    return this.metaData;
  }

  public void setDynamicAttributeToken(String dynamicAttributeToken) {
    this.dynamicAttributeToken = dynamicAttributeToken;
  }

  public String getDynamicAttributeToken() {
    return this.dynamicAttributeToken;
  }
}
