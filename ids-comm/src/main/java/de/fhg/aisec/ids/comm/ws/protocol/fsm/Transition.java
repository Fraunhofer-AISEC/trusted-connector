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

import de.fhg.aisec.ids.comm.ws.protocol.ProtocolState;
import java.util.function.Function;
import org.slf4j.LoggerFactory;

/**
 * A Transition transfers the FSM from a start state to an end state and is triggered by an event.
 *
 * <p>Associated with a transition is code that is executed when the transition is taken.
 */
public class Transition {
  final Object event;
  final String startState;
  final String endState;
  final Function<Event, Boolean> runBeforeTransition;

  /**
   * Creates a new transition.
   *
   * @param eventName the event triggering this transition.
   * @param startState the start state of this transition.
   * @param endState the end state of this transition.
   * @param runBeforeTransition code executed when the transition is triggered. If the callable
   *     returns true, the transition takes place, if the callable returns false, the transition
   *     will not take place and the FSM remains in startState.
   */
  public Transition(
      String eventName,
      String startState,
      String endState,
      Function<Event, Boolean> runBeforeTransition) {
    this.event = eventName;
    this.startState = startState;
    this.endState = endState;
    this.runBeforeTransition = runBeforeTransition;
  }

  public Transition(
      Object evtKey,
      String startState,
      String endState,
      Function<Event, Boolean> runBeforeTransition) {
    this.event = evtKey;
    this.startState = startState;
    this.endState = endState;
    this.runBeforeTransition = runBeforeTransition;
  }

  public Transition(
      Object evtKey,
      ProtocolState startState,
      ProtocolState endState,
      Function<Event, Boolean> runBeforeTransition) {
    this.event = evtKey;
    this.startState = startState.id();
    this.endState = endState.id();
    this.runBeforeTransition = runBeforeTransition;
  }

  /**
   * Executes code associated with this transition.
   *
   * <p>This method returns the result of the executed Callable (true or false) or false in case of
   * any Exceptions. No exceptions will be thrown from this method.
   *
   * @return Whether
   */
  protected boolean doBeforeTransition(Event event) {
    if (runBeforeTransition != null) {
      try {
        runBeforeTransition.apply(event);
      } catch (Throwable t) {
        LoggerFactory.getLogger(this.getClass().getName())
            .warn("Error in before-transition-procedure", t);
        return false;
      }
    }
    return true;
  }
}
