/*-
 * ========================LICENSE_START=================================
 * LUCON Data Flow Policy Engine
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.ids.dataflowcontrol.lucon;

import alice.tuprolog.Library;
import alice.tuprolog.Number;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Plugins and default theories for tuProlog engine.
 * 
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 *
 */
public class LuconLibrary extends Library {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(LuconLibrary.class);

    private LoadingCache<String, Pattern> regexCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumWeight((long) 1e6).weigher((k, v) -> ((String) k).length())
            .build(new CacheLoader<String, Pattern>() {
                @ParametersAreNonnullByDefault
                public Pattern load(String key) {
                    return Pattern.compile(key);
                }
            });

    @Override
    public String getTheory() {
        return
        "set_of(In, Out) :-  % get a pairwise different, sorted set from a list\n" +
        "  quicksort(In, '@<', Sorted),\n" +
        "  no_duplicates(Sorted, Out).\n" +
        "\n" +
        "get_labels(Out) :-  % collect and return asserted labels\n" +
        "  once(setof(L, label(L), Out) ; Out = []).\n" +
        "\n" +
        "assert_labels(L, A) :- assert_labels(L, A, []).\n" +
        "assert_labels([], A, A).\n" +
        "assert_labels([L|Tail], Ar, A) :- label(L), assert_labels(Tail, Ar, A), !.\n" +
        "assert_labels([L|Tail], Ar, A) :- assert(label(L)), assert_labels(Tail, Ar, [L|A]).\n" +
        "\n" +
        "retract_labels(L, R) :- retract_labels(L, R, []).\n" +
        "retract_labels([], R, R).\n" +
        "retract_labels([L|Tail], Rr, R) :- retract(label(L)), retract_labels(Tail, Rr, [L|R]), !.\n" +
        "retract_labels([L|Tail], Rr, R) :- retract_labels(Tail, Rr, R).\n" +
        "\n" +
        "all_ground([]).\n" +
        "all_ground([Head|Tail]) :- ground(Head), all_ground(Tail).\n" +
        "\n" +
        "cache_put_all_unsafe(KL, []).\n" +
        "cache_put_all_unsafe(KL, [V|T]) :- cache_put_unsafe(KL, V), cache_put_all_unsafe(KL, T).\n" +
        "cache_put_unsafe(KL, V) :-\n" +
        "  %print([\"CACHE PUT: \"|[KL, V]]), nl,\n" +
        "  assertz(cache_entry(KL, V)).\n" +
        "\n" +
        "cache_put_all(KL, VL) :- all_ground(KL), cache_put_all_unsafe(KL, VL).\n" +
        "cache_put(KL, V) :- cache_put_check(KL, V); true.\n" +
        "cache_put_check(KL, V) :- all_ground(KL), cache_put_unsafe(KL, V).\n" +
        "\n" +
        "cache_get(KL, V) :-\n" +
        "  cache_entry(KL, V).\n" +
        "  %-> print([\"CACHE HIT: \", KL, V]), nl\n" +
        "  %; print([\"CACHE MISS: \", KL]), nl, fail.\n" +
        "\n" +
        "cache_clear(KL) :- retractall(cache_entry(KL, _)).\n" +
        "\n" +
        "action_service(Action, S) :-  % Finds services S matching endpoints of N  [ O(|Ep_S|) ]\n" +
        "  has_endpoint(S, Regex),       % a service S exists such that  [ O(|Ep_S|) ]\n" +
        "  regex_match(Regex, Action).   % the action of A matches the endpoint of S  [ assume O(1) ]\n" +
        "\n" +
        "collect_creates_labels([], []).\n" +
        "collect_creates_labels([S|SCTail], ACout) :-\n" +
        "  collect_creates_labels(SCTail, ACnew),\n" +
        "  findall(A, creates_label(S, A), AC),\n" +
        "  once(bound(ACnew); ACnew = []),\n" +
        "  append(AC, ACnew, ACout).\n" +
        "\n" +
        "collect_removes_labels([], []).\n" +
        "collect_removes_labels([S|SCTail], RCout) :-\n" +
        "  collect_removes_labels(SCTail, RCnew),\n" +
        "  findall(R, removes_label(S, R), RC),\n" +
        "  once(bound(RCnew); RCnew = []),\n" +
        "  append(RC, RCnew, RCout).\n" +
        "\n" +
        "update_labels(Act, Out, Aout, Rout) :-                                             % Updates labels according to spec. of service S\n" +
        "  once(setof(S, action_service(Act, S), SC); SC = []),                             % collect all relevant services\n" +
        "  collect_creates_labels(SC, ACraw), set_of(ACraw, AC), assert_labels(AC, Aout),   % assert new labels added by S\n" +
        "  collect_removes_labels(SC, RCraw), set_of(RCraw, RC), retract_labels(RC, Rout),  % retract labels removed by S\n" +
        "  %print([Act, \"=>\", SC, \"added:\", Aout, \"removed:\", Rout]),nl,\n" +
        "  get_labels(Out).                                                                 % collect and return asserted labels\n" +
        "\n" +
        "dominant_rules(Act, Req, DC, S, R) :-  % Find the dominant rule R for action Act (from cache)\n" +
        "  get_labels(LC),                        % Collect currently asserted labels\n" +
        "  cache_get([dr, Act, Req, DC, LC], V),  % CACHE GET entry\n" +
        "  list(V), V = [S, R].                   % unpack result\n" +
        "\n" +
        "dominant_rules(Act, Req, DC, S, R) :-  % Find the dominant rule R for action Act  [ O(|Ep_S| x |S -- R|) ]\n" +
        "  get_labels(LC),                            % Collect currently asserted labels\n" +
        "  \\+(cache_get([dr, Act, Req, DC, LC], V)),  % CACHE GET entry\n" +
        "  (setof(DomRule, (\n" +
        "    action_service(Act, Si),                   % Action Act is matched by a service Si  [ O(|Ep_S|) ]\n" +
        "    rule(Ri), receives_label(Ri),              % There is a VALID rule Ri\n" +
        "    has_target(Ri, Si),                        % targeting by service Si  [ O(|S -- R|) ]\n" +
        "    has_decision(Ri, Req),                     % with a decision that unifies with Require  [ O(1) ]\n" +
        "    rule_priority(Ri, PR),                     % that has priority PR, then  [ O(1) ]\n" +
        "    \\+(                                        % there MUST NOT exist  [ O(|Ep_S| x |S -- R|) ]\n" +
        "      action_service(Act, S2),                   % a service S2  [ O(|Ep_S|) ]\n" +
        "      rule(R2), Ri \\= R2, receives_label(R2),    % with another VALID rule R2\n" +
        "      has_target(R2, S2),                        % that is targeting S2  [ O(|S -- R|) ]\n" +
        "      has_decision(R2, D2), D2 \\= Req,           % which enforces a different decision,  [ O(1) ]\n" +
        "      rule_priority(R2, PR2),                    % and has priority PR2  [ O(1) ]\n" +
        "      G =.. [DC, PR2, PR], call(G)               % such that 'DC'(PR, PR2) is fulfilled  [ O(1) ]\n" +
        "    ), DomRule = [Si, Ri]                      % compose the result\n" +
        "  ), RL)\n" +
        "  -> cache_put_all([dr, Act, Req, DC, LC], RL)   % IF successful, CACHE PUT ALL results\n" +
        "  ; cache_put([dr, Act, Req, DC, LC], none)      % ELSE CACHE PUT 'none'\n" +
        "  ), !,                                      % don't backtrack into the caching logic\n" +
        "  dominant_rules(Act, Req, DC, S, R).        % delegate to cache handler\n" +
        "\n" +
        "dominant_drop_rules(Act, S, R) :- dominant_rules(Act, drop, '>', S, R).\n" +
        "dominant_allow_rules(Act, S, R) :- dominant_rules(Act, allow, '>=', S, R).\n" +
        "\n" +
        "% Query for a path between two nodes and print the labels along the possible paths like so:\n" +
        "%\n" +
        "%   path(stmt_1, stmt_5, Trace).\n" +
        "%\n" +
        "path(A,B,T) :-                              % Two nodes are connected if we can walk from A to B,\n" +
        "  trace_walk(A, B, [], [[A, []]], T).       % starting with empty label list\n" +
        "\n" +
        "trace_walk(A, B, L, Log, T) :-              % We have walked from A to B and verify  [ O(|Ep_S| x |S -- R|) ]\n" +
        "  A = B,                                    %   A is the desired destination with  [ O(1) ]\n" +
        "  has_action(A, Act),                       %   an action Act and there is  [ O(1) ]\n" +
        "  dominant_drop_rules(Act, S, R),           %   a dominant drop rule R and service S for Act [ O(|Ep_S| x |S -- R|) ]\n" +
        "  receives_label(R),                        %     R receives a set of labels with  [ assume O(1) ]\n" +
        "  get_labels(LC),                           %     get asserted labels  [ O(L_a), assume O(1) ]\n" +
        "  T = [[S, LC, R]|Log].                     %     [unify the recursion result with Out]  [ O(1) ]\n" +
        "  %print(\"finished (END): \"), print(A), nl.\n" +
        "\n" +
        "trace_walk(A, B, L, Log, T) :-              % We can walk from A to B if  [ O(|Ep_S| x |S -- R|) ]\n" +
        "  succ(A, X),                               %   A is connected to X and there is  [ O(|succ(A, _)|), assume O(1) ]\n" +
        "  has_action(A, Act),                       %   an action Action and there is  [ O(1) ]\n" +
        "  dominant_allow_rules(Act, S, _),          %   a dominant allow rule and service S for Act  [ O(|Ep_S| x |S -- R|) ]\n" +
        "  update_labels(Act, LN, Aout, Rout),       %   [update the labels for the next step]  [ O(|L|), assume O(1) ],\n" +
        "  %print([Aout, Rout]), nl,\n" +
        "  %print(\"transition: \"), print([A, X, S, LN]), nl,\n" +
        "  (\n" +
        "    trace_walk(X, B, LN, [[X, LN]|Log], T)  %   we can get from X to B  [ Recursion! ]\n" +
        "    ; true                                  %   or otherwise, make sure that the cleanup stuff below gets called!\n" +
        "  ),\n" +
        "  retract_labels(Aout, _),                  %   retract labels asserted before recursion\n" +
        "  assert_labels(Rout, _),                   %   assert labels retracted before recursion\n" +
        "  %print(\"finished: \"), print(A), nl,\n" +
        "  ground(T).                                %   If T is bound, recursion returned successfully, no result otherwise!\n";
    }

    private static boolean isComplex(@NonNull Term t) {
        if (t instanceof Var) {
            t = t.getTerm();
        }
        return (!t.isAtom() || t.isList()) && !(t instanceof Number);
    }

    @SuppressWarnings("unused")
    public boolean regex_match_2(Term regex, Term input) {
    	LOG.trace("regex_match/2 called with " + regex + " " + input);
    	// Both regex and input string must be ground
        if (isComplex(regex) || isComplex(input)) {
            return false;
        }
        try {
        	String regexString = TuPrologHelper.unquote(regex.getTerm().toString());
        	String inputString = TuPrologHelper.unquote(input.getTerm().toString());
            boolean match = regexCache.get(regexString)
                    .matcher(inputString).matches();
            LOG.trace("regex_match: " + regexString + " , " + inputString + ": " + match);
            return match;
        } catch (ExecutionException e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
    }
}