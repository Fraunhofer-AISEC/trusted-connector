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
        return "union(LIST,[],LIST).\n" +
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
                "% Query for a path between two nodes and print the labels along the possible paths like so:\n" +
                "%\n" +
                "%   path(stmt_1, stmt_5).\n" +
                "%\n" +
                "path(A,B,T) :-                                % Two nodes are connected if we can walk from A to B,\n" +
                "  taint_walk(A,B,[],[],[[A,[]]],T).           % starting with empty lists for labels, visited nodes and trace.\n" +
                "  % print('END TRACE'),nl.                    % Mark the end of a successful walk.\n" +
                "\n" +
                "taint_walk(A,B,V,C,Log,T) :-                  % we can walk from A to B, maintaining context taint marks in C\n" +
                "  has_action(A, Action),                      % if A has an action and\n" +
                "  has_endpoint(S, Regex),                     % a service S exists such that\n" +
                "  regex_match(Regex, Action),                 % the action of A matches the endpoint of S\n" +
                "  (                                           % - either\n" +
                "    A = B,                                    %   - A is the desired destination and\n" +
                "    rule(R), has_decision(R, drop), has_target(R,S), receives_label(R, Forbidden), intersects(Forbidden, C),\n" +
                "    % print(R),nl,                            %   - taint policy forbids a flow to B\n" +
                "    % print(['reason: service ', S, ' receives label(s) ', Forbidden, ' which is forbidden by ', R]),nl,\n" +
                "    T = [[S, Forbidden, R]|Log]\n" +
                "    ;                                         % - OR\n" +
                "    creates_label(S, ADDED),                  % taint flags added by A\n" +
                "    union(ADDED,C,C_ADDED),                   % add new taint flags to list\n" +
                "    removes_label(S, REMOVED),                % taint flags removed by A\n" +
                "    deletelist(C_ADDED, REMOVED, C_NEW),      % remove removed taint flags from list\n" +
                "    succ(A,X),                                %   - if A is connected to X, and\n" +
                "    not(member(X,V)),                         %   - we haven't yet visited X, and\n" +
                "    taint_walk(X,B,[A|V],C_NEW,[[X, C_NEW]|Log],T)  %   - we can get to it from X\n" +
                "    % print([X, C_NEW]),nl                      % output state after recursion\n" +
                "  ).\n";
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
            return regexCache.get(TuPrologHelper.unescape(regex.getTerm().toString()))
                    .matcher(TuPrologHelper.unescape(input.getTerm().toString())).matches();
        } catch (ExecutionException ee) {
            ee.printStackTrace();
            return false;
        }
    }

}
