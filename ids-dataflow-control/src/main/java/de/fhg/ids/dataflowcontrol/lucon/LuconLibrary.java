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
import de.fhg.ids.dataflowcontrol.PolicyDecisionPoint;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LuconLibrary extends Library {
	private static final Logger LOG = LoggerFactory.getLogger(LuconLibrary.class);

    @Override
    public String getTheory() {
        return
        "union(LIST,[],LIST).\n" +
        "union(LIST,[A|TAIL],RESULT) :- member(A, LIST), union(LIST,TAIL,RESULT), !.\n" +
        "union(LIST,[A|TAIL],[A|RESULT]) :- union(LIST,TAIL,RESULT).\n" +
        "\n" +
        "deletelist([], _, []).\n" +
        "deletelist([X|Xs], Y, Z) :- member(X, Y), deletelist(Xs, Y, Z), !.\n" +
        "deletelist([X|Xs], Y, [X|Zs]) :- deletelist(Xs, Y, Zs).\n" +
        "\n" +
        "% Helper function: Is intersection of lists non-empty?\n" +
        "intersects([H|_],List) :- member(H,List), !.\n" +
        "intersects([_|T],List) :- intersects(T,List).\n" +
        "\n" +
        "update_labels(In, S, Out) :-                                 % Updates labels according to spec. of service S\n" +
        "  once(creates_label(S, A); A = []), union(In, A, I),        % adding new labels added by S\n" +
        "  once(removes_label(S, R); R = []), deletelist(I, R, Out).  % and removing labels removed by S\n" +
        "\n" +
        "all_ground([]).\n" +
        "all_ground([Head|Tail]) :- ground(Head), all_ground(Tail).\n" +
        "\n" +
        "cache_put_all_unsafe(KL, []).\n" +
        "cache_put_all_unsafe(KL, [V|T]) :- cache_put_unsafe(KL, V), cache_put_all_unsafe(KL, T).\n" +
        "cache_put_unsafe(KL, V) :- \n" +
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
        "dominant_rules(Act, Req, DC, S, R) :-  % Find the dominant rule R for action Act (from cache)\n" +
        "  cache_get([dr, Act, Req, DC], V),      % CACHE GET entry\n" +
        "  list(V), V = [S, R].                   % unpack result\n" +
        "\n" +
        "dominant_rules(Act, Req, DC, S, R) :-  % Find the dominant rule R for action Act  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  \\+(cache_get([dr, Act, Req, DC], V)),     % CACHE GET entry\n" +
        "  (setof(DomRule, (\n" +
        "    action_service(Act, Si),                  % Action Act is matched by a service S  [ O(|Ep_S|) ]\n" +
        "    rule(Ri), has_target(Ri, Si),             % There is a rule R for service S  [ O(|S -- R|) ]\n" +
        "    has_decision(Ri, Req),                    % with a decision that unifies with Require  [ O(1) ]\n" +
        "    rule_priority(Ri, PR),                    % that has priority PR, then  [ O(1) ]\n" +
        "    \\+(                                       % there MUST NOT exist  [ O(|Ep_S| x |S -- R|) ]\n" +
        "      action_service(Act, S2),                  % a service S2  [ O(|Ep_S|) ]\n" +
        "      rule(R2), Ri \\= R2, has_target(R2, S2),   % targeted by another rule R2  [ O(|S -- R|) ]\n" +
        "      has_decision(R2, D2), D2 \\= Req,          % which enforces a different decision,  [ O(1) ]\n" +
        "      rule_priority(R2, PR2),                   % and has priority PR2  [ O(1) ]\n" +
        "      G =.. [DC, PR2, PR], call(G)              % such that 'DC'(PR, PR2) is fulfilled  [ O(1) ]\n" +
        "    ), DomRule = [Si, Ri]                     % compose the result\n" +
        "  ), RL)\n" +
        "  -> cache_put_all([dr, Act, Req, DC], RL)  % IF successful, CACHE PUT ALL results\n" +
        "  ; cache_put([dr, Act, Req, DC], none)     % ELSE CACHE PUT 'none'\n" +
        "  ), !,                                  % don't backtrack into the caching logic\n" +
        "  dominant_rules(Act, Req, DC, S, R).    % delegate to cache handler\n" +
        "\n" +
        "dominant_drop_rules(Act, S, R) :- dominant_rules(Act, drop, '>', S, R).\n" +
        "dominant_allow_rules(Act, S, R) :- dominant_rules(Act, allow, '>=', S, R).\n" +
        "\n" +
        "% Query for a path between two nodes and print the labels along the possible paths like so:\n" +
        "%\n" +
        "%   path(stmt_1, stmt_5, Trace).\n" +
        "%\n" +
        "path(A,B,T) :-                    % Two nodes are connected if we can walk from A to B,\n" +
        "  trace_walk(A,B,[],[[A,[]]],T).  % starting with empty label list\n" +
        "\n" +
        "trace_walk(A, B, L, Log, T) :-             % We have walked from A to B and verify  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  A = B,                                   %   A is the desired destination with  [ O(1) ]\n" +
        "  has_action(A, Act),                      %   an action Act and there is  [ O(1) ]\n" +
        "  dominant_drop_rules(Act, S, R),          %   a dominant drop rule R and service S for Act [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  (                                        %   AND\n" +
        //"    receives_label(R, any),                %     R receives either any set of labels  [ O(1) ]\n" +
        "	 assert(any), " +
        "    T = [[S, any, R]|Log]                  %     [unify the recursion result with Out]  [ O(1) ]\n" +
        "    ;                                      %     or\n" +
        //"    receives_label(R, Forbidden),          %     R receives a set of labels with  [ O(1) ]\n" +
        "	 assert(Forbidden), " +
        "    intersects(Forbidden, L),              %     non-empty intersection with C  [ O(|Forbidden|), assume O(1) ]\n" +
        "    T = [[S, Forbidden, R]|Log]            %     [unify the recursion result with Out]  [ O(1) ]\n" +
        "  ).                                       %\n" +
        "  %, print(A),nl.\n" +
        "\n" +
        "trace_walk(A, B, L, Log, T) :-             % We can walk from A to B if  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  succ(A,X),                               %   A is connected to X and there is  [ O(|succ(A, _)|), assume O(1) ]\n" +
        "  has_action(A, Act),                      %   an action Action and there is  [ O(1) ]\n" +
        "  dominant_allow_rules(Act, S, _),         %   a dominant allow rule and service S for Act  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  update_labels(L, S, LN),                 %   [update the labels for the next step]  [ O(|L|), assume O(1) ],\n" +
        "  %print([\"transition: \", A, X, S, LN]), nl,\n" +
        "  trace_walk(X, B, LN, [[X, LN]|Log], T).  %   we can get from X to B  [ Recursion! ]\n" +
        "  %, print(A),nl.\n";
    }

    private LoadingCache<String, Pattern> regexCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .maximumWeight((long) 1.e6).weigher((k, v) -> ((String) k).length())
            .build(new CacheLoader<String, Pattern>() {
                public Pattern load(String key) {
                    return Pattern.compile(key);
                }
            });

    private static boolean isComplex(Term t) {
        if (t instanceof Var) {
            t = t.getTerm();
            t.isCompound();
        }
        return (!t.isAtom() || t.isList()) && !(t instanceof Number);
    }

    @SuppressWarnings("unused")
    public boolean regex_match_2(Term regex, Term input) {
        // Both regex and input string must be ground
        if (isComplex(regex) || isComplex(input)) {
            return false;
        }
        try {
        	String regexString = TuPrologHelper.unquote(regex.getTerm().toString());
        	String inputString = TuPrologHelper.unquote(input.getTerm().toString());
            boolean match = regexCache.get(regexString)
                    .matcher(inputString).matches();
            LOG.trace("regex_match_2: " + regexString + " , " + inputString + ": " + match);
            return match;
        } catch (ExecutionException e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
    }

}
