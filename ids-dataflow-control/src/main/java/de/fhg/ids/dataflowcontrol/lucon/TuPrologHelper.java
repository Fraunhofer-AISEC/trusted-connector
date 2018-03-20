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

import alice.tuprolog.Prolog;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TuPrologHelper {

    /**
     * Set with weak keys, such that unused VMs may be garbage-collected with the referencing thread
     */
    private static final Set<Prolog> vms = Collections.newSetFromMap(new WeakHashMap<Prolog, Boolean>());

    private static final ThreadLocal<Prolog> threadProlog = ThreadLocal.withInitial(Prolog::new);

    public static Prolog getVm() {
        return threadProlog.get();
    }

    @NonNull
    public static String escape(@NonNull String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        int charLength = s.length();
        for (int i = 0; i < charLength; i++) {
            char c = s.charAt(i);
            sb.append(c == '\'' ? "''" : c);
        }
        sb.append('\'');
        return sb.toString();
    }

	@NonNull
    public static Stream<? extends Term> listStream(@Nullable Term list) {
        if (list == null) {
        	return Stream.empty();
        }
    	if (!list.isList()) {
            throw new IllegalArgumentException("Not a tuProlog list");
        }
        Iterator<? extends Term> listIterator = ((Struct) list).listIterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(listIterator, Spliterator.ORDERED),
                false);
    }

    @Nullable
    public static String unquote(@Nullable String s) {
    	if (s == null) {
    		return null;
    	}
        if (s.length() > 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            return s.substring(1, s.length() - 1);
        } else if (s.length() == 2 && "''".equals(s)) {
            return "";
        } else {
            return s;
        }
    }
}