/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
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
package de.fhg.aisec.ids.dataflowcontrol.lucon

import alice.tuprolog.Library
import alice.tuprolog.Number
import alice.tuprolog.Term
import alice.tuprolog.Var
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Plugins and default theories for tuProlog engine.
 *
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
@Suppress("FunctionName", "unused")
class LuconLibrary : Library() {
    @Transient
    private val regexCache =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumWeight(1e6.toLong())
            .weigher<String, Regex> { k, _ -> k.length }
            .build(
                object : CacheLoader<String, Regex>() {
                    override fun load(key: String) = Regex(key, RegexOption.DOT_MATCHES_ALL)
                }
            )

    override fun getTheory(): String =
        """
        set_of(In, Out) :-  % get a pairwise different, sorted set from a list
          quicksort(In, '@<', Sorted),
          no_duplicates(Sorted, Out).

        get_labels(Out) :-  % collect and return asserted labels
          once(setof(L, label(L), Out) ; Out = []).

        assert_labels(L, A) :- assert_labels(L, A, []).
        assert_labels([], A, A).
        assert_labels([L|Tail], Ar, A) :- label(L), assert_labels(Tail, Ar, A), !.
        assert_labels([L|Tail], Ar, A) :- assert(label(L)), assert_labels(Tail, Ar, [L|A]).

        retract_labels(L, R) :- retract_labels(L, R, []).
        retract_labels([], R, R).
        retract_labels([L|Tail], Rr, R) :- retract(label(L)), retract_labels(Tail, Rr, [L|R]), !.
        retract_labels([_|Tail], Rr, R) :- retract_labels(Tail, Rr, R).

        all_ground([]).
        all_ground([Head|Tail]) :- ground(Head), all_ground(Tail).

        cache_put_all_unsafe(_, []).
        cache_put_all_unsafe(KL, [V|T]) :- cache_put_unsafe(KL, V), cache_put_all_unsafe(KL, T).
        cache_put_unsafe(KL, V) :-
          %print(["CACHE PUT: "|[KL, V]]), nl,
          assertz(cache_entry(KL, V)).

        cache_put_all(KL, VL) :- all_ground(KL), cache_put_all_unsafe(KL, VL).
        cache_put(KL, V) :- cache_put_check(KL, V); true.
        cache_put_check(KL, V) :- all_ground(KL), cache_put_unsafe(KL, V).

        cache_get(KL, V) :-
          cache_entry(KL, V).
          %-> print(["CACHE HIT: ", KL, V]), nl
          %; print(["CACHE MISS: ", KL]), nl, fail.

        cache_clear(KL) :- retractall(cache_entry(KL, _)).

        action_service(Action, S) :-  % Finds services S matching endpoints of N  [ O(|Ep_S|) ]
          has_endpoint(S, Regex),       % a service S exists such that  [ O(|Ep_S|) ]
          regex_match(Regex, Action).   % the action of A matches the endpoint of S  [ assume O(1) ]

        collect_creates_labels([], []).
        collect_creates_labels([S|SCTail], ACout) :-
          collect_creates_labels(SCTail, ACnew),
          findall(A, creates_label(S, A), AC),
          once(bound(ACnew); ACnew = []),
          append(AC, ACnew, ACout).

        collect_removes_labels([], []).
        collect_removes_labels([S|SCTail], RCout) :-
          collect_removes_labels(SCTail, RCnew),
          findall(R, removes_label(S, R), RC),
          once(bound(RCnew); RCnew = []),
          append(RC, RCnew, RCout).

        update_labels(Act, Out, Aout, Rout) :-                                             % Updates labels according to spec. of service S
          once(setof(S, action_service(Act, S), SC); SC = []),                             % collect all relevant services
          collect_creates_labels(SC, ACraw), set_of(ACraw, AC), assert_labels(AC, Aout),   % assert new labels added by S
          collect_removes_labels(SC, RCraw), set_of(RCraw, RC), retract_labels(RC, Rout),  % retract labels removed by S
          print([Act, "=>", SC, "added:", Aout, "removed:", Rout]),nl,
          get_labels(Out).                                                                 % collect and return asserted labels

        conflicting_rules(Act, Ri, Req, DC, PR) :-   % Find conflicting rules  [ O(|Ep_S| x |S -- R|) ]
          action_service(Act, S2),                   % a service S2  [ O(|Ep_S|) ]
          rule(R2), Ri \= R2, receives_label(R2),    % with another VALID rule R2
          has_target(R2, S2),                        % that is targeting S2  [ O(|S -- R|) ]
          has_decision(R2, D2), D2 \= Req,           % which enforces a different decision,  [ O(1) ]
          rule_priority(R2, PR2),                    % and has priority PR2  [ O(1) ]
          G =.. [DC, PR2, PR], call(G).              % such that 'DC'(PR, PR2) is fulfilled  [ O(1) ]

        dominant_rules(Act, Req, DC, S, R) :-    % Find the dominant rule R for action Act (from cache)
          get_labels(LC),                        % Collect currently asserted labels
          cache_get([dr, Act, Req, DC, LC], V),  % CACHE GET entry
          list(V), V = [S, R].                   % unpack result

        dominant_rules(Act, Req, DC, S, R) :-        % Find the dominant rule R for action Act  [ O(|Ep_S| x |S -- R|) ]
          get_labels(LC),                            % Collect currently asserted labels
          \+(cache_get([dr, Act, Req, DC, LC], _)),  % CACHE GET entry
          (setof(DomRule, (
            action_service(Act, Si),                      % Action Act is matched by a service Si  [ O(|Ep_S|) ]
            rule(Ri), receives_label(Ri),                 % There is a VALID rule Ri
            has_target(Ri, Si),                           % targeting by service Si  [ O(|S -- R|) ]
            has_decision(Ri, Req),                        % with a decision that unifies with Require  [ O(1) ]
            rule_priority(Ri, PR),                        % that has priority PR, then  [ O(1) ]
            \+(conflicting_rules(Act, Ri, Req, DC, PR)),  % there MUST NOT exist conflicting rules  [ O(|Ep_S| x |S -- R|) ]
            DomRule = [Si, Ri]                            % compose the result
          ), RL)
          -> cache_put_all([dr, Act, Req, DC, LC], RL)   % IF successful, CACHE PUT ALL results
          ; cache_put([dr, Act, Req, DC, LC], none)      % ELSE CACHE PUT 'none'
          ), !,                                      % don't backtrack into the caching logic
          dominant_rules(Act, Req, DC, S, R).        % delegate to cache handler

        dominant_drop_rules(Act, S, R) :- dominant_rules(Act, drop, '>', S, R).
        dominant_allow_rules(Act, S, R) :- dominant_rules(Act, allow, '>=', S, R).

        % Query for a path between two nodes and print the labels along the possible paths like so:
        %
        %   path(stmt_1, stmt_5, Trace).
        %
        path(A,B,T) :-                              % Two nodes are connected if we can walk from A to B,
          trace_walk(A, B, [[A, []]], T).       % starting with empty label list

        trace_walk(A, B, Log, T) :-              % We have walked from A to B and verify  [ O(|Ep_S| x |S -- R|) ]
          A = B,                                    %   A is the desired destination with  [ O(1) ]
          has_action(A, Act),                       %   an action Act and there is  [ O(1) ]
          dominant_drop_rules(Act, S, R),           %   a dominant drop rule R and service S for Act [ O(|Ep_S| x |S -- R|) ]
          receives_label(R),                        %     R receives a set of labels with  [ assume O(1) ]
          get_labels(LC),                           %     get asserted labels  [ O(L_a), assume O(1) ]
          T = [[S, LC, R]|Log].                     %     [unify the recursion result with Out]  [ O(1) ]
          %print("finished (END): "), print(A), nl.

        trace_walk(A, B, Log, T) :-              % We can walk from A to B if  [ O(|Ep_S| x |S -- R|) ]
          succ(A, X),                               %   A is connected to X and there is  [ O(|succ(A, _)|), assume O(1) ]
          has_action(A, Act),                       %   an action Action and there is  [ O(1) ]
          dominant_allow_rules(Act, _, _),          %   a dominant allow rule and service S for Act  [ O(|Ep_S| x |S -- R|) ]
          update_labels(Act, LN, Aout, Rout),       %   [update the labels for the next step]  [ O(|L|), assume O(1) ],
          %print([Aout, Rout]), nl,
          %print("transition: "), print([A, X, S, LN]), nl,
          (
            trace_walk(X, B, [[X, LN]|Log], T)  %   we can get from X to B  [ Recursion! ]
            ; true                                  %   or otherwise, make sure that the cleanup stuff below gets called!
          ),
          retract_labels(Aout, _),                  %   retract labels asserted before recursion
          assert_labels(Rout, _),                   %   assert labels retracted before recursion
          %print("finished: "), print(A), nl,
          ground(T).                                %   If T is bound, recursion returned successfully, no result otherwise!
        """.trimIndent()

    fun regex_match_2(
        regex: Term,
        input: Term
    ): Boolean {
        if (LOG.isTraceEnabled) {
            LOG.trace("regex_match/2 called with $regex $input")
        }
        // Both regex and input string must be ground
        return if (isComplex(regex) || isComplex(input)) {
            false
        } else {
            try {
                val regexString = TuPrologHelper.unquote(regex.term.toString())
                val inputString = TuPrologHelper.unquote(input.term.toString())
                val match = regexCache[regexString].matches(inputString)
                if (LOG.isTraceEnabled) {
                    LOG.trace("regex_match: $regexString , $inputString: $match")
                }
                match
            } catch (e: ExecutionException) {
                if (LOG.isWarnEnabled) {
                    LOG.warn(e.message, e)
                }
                false
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LuconLibrary::class.java)

        private fun isComplex(term: Term): Boolean {
            var t = term
            if (t is Var) {
                t = t.getTerm()
            }
            return (!t.isAtom || t.isList) && t !is Number
        }
    }
}
