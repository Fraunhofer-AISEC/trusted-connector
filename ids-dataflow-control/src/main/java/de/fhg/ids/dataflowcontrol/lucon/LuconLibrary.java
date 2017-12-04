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
import alice.tuprolog.Term;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class LuconLibrary extends Library {

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
        "update_labels(In, S, Out) :-                   % Updates labels according to spec. of service S\n" +
        "  creates_label(S, A), union(In, A, I),        % adding new labels added by S\n" +
        "  removes_label(S, R), deletelist(I, R, Out).  % and removing labels removed by S\n" +
        "\n" +
        "action_service(Action, S) :-   % Finds services S matching endpoints of N  [ O(|Ep_S|) ]\n" +
        "  has_endpoint(S, Regex),      % a service S exists such that  [ O(|Ep_S|) ]\n" +
        "  regex_match(Regex, Action).  % the action of A matches the endpoint of S  [ assume O(1) ]\n" +
        "\n" +
        "dominant_rule(Act, Req, S, R) :-  % Find the dominant rule R for node A  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  action_service(Act, S),              % action of A is matched by a service S  [ O(|Ep_S|) ]\n" +
        "  rule(R), has_target(R, S),           %   There is a rule R for service S  [ O(|S -- R|) ]\n" +
        "  has_decision(R, Req),                %   with a decision that unifies with Require  [ O(1) ]\n" +
        "  rule_priority(R, PR),                %   that has priority PR, then  [ O(1) ]\n" +
        "  \\+(                                 %   there MUST NOT exist  [ O(|Ep_S| x |S -- R|) ]\n" +
        "    action_service(Act, S2),           %     another service S2  [ O(|Ep_S|) ]\n" +
        "    rule(R2), has_target(R2, S2),      %     targeted by a rule R2,  [ O(|S -- R|) ]\n" +
        "    R \\= R2,                          %     that is not equal to R and  [ O(1) ]\n" +
        "    rule_priority(R2, PR2),            %     has priority PR2, such that  [ O(1) ]\n" +
        "    PR2 > PR                           %     the priority PR2 is greather than PR  [ O(1) ]\n" +
        "  ).                                   %\n" +
        "\n" +
        "dominant_drop_rule(Act, S, R) :- dominant_rule(Act, drop, S, R).\n" +
        "dominant_allow_rule(Act, S, R) :- dominant_rule(Act, allow, S, R).\n" +
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
        "  dominant_drop_rule(Act, S, R),           %   a dominant drop rule R and service S for Act [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  (                                        %   AND\n" +
        "    receives_label(R, any),                %     R receives either any set of labels  [ O(1) ]\n" +
        "    T = [[S, any, R]|Log]                  %     [unify the recursion result with Out]  [ O(1) ]\n" +
        "    ;                                      %     or\n" +
        "    receives_label(R, Forbidden),          %     R receives a set of labels with  [ O(1) ]\n" +
        "    intersects(Forbidden, L),              %     non-empty intersection with C  [ O(|Forbidden|), assume O(1) ]\n" +
        "    T = [[S, Forbidden, R]|Log]            %     [unify the recursion result with Out]  [ O(1) ]\n" +
        "  ).                                       %\n" +
        "  %, print(A),nl.\n" +
        "\n" +
        "trace_walk(A, B, L, Log, T) :-             % We can walk from A to B if  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  succ(A,X),                               %   A is connected to X and there is  [ O(|succ(A, _)|), assume O(1) ]\n" +
        "  has_action(A, Act),                      %   an action Action and there is  [ O(1) ]\n" +
        "  dominant_allow_rule(Act, S, _),          %   a dominant allow rule and service S for Act  [ O(|Ep_S|² x |S -- R|²) ]\n" +
        "  update_labels(L, S, LN),                 %   [update the labels for the next step]  [ O(|L|), assume O(1) ]\n" +
        "  trace_walk(X, B, LN, [[X, LN]|Log], T).  %   we can get from X to B  [ Recursion! ]\n" +
        "  %, print(A),nl.\n";
    }

    private LoadingCache<String, Pattern> regexCache = CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Pattern>() {
                public Pattern load(@Nonnull String key) {
                    return Pattern.compile(key);
                }
            });

    @SuppressWarnings("unused")
    public boolean regex_match_2(Term regex, Term input) {
        try {
            return regexCache.get(TuPrologHelper.unquote(regex.getTerm().toString()))
                    .matcher(TuPrologHelper.unquote(input.getTerm().toString())).matches();
        } catch (ExecutionException ee) {
            ee.printStackTrace();
            return false;
        }
    }

}
