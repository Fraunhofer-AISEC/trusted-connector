/*-
 * ========================LICENSE_START=================================
 * IDS Route Manager
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
package de.fhg.aisec.ids.api.router.graph;

import java.util.LinkedHashSet;
import java.util.Set;

public class GraphData {
    private Set<Node> nodes = new LinkedHashSet<>();
    private Set<Edge> links = new LinkedHashSet<>();

    public void addNode(Node node) {
        nodes.add(node);
    }

    public void addEdge(Edge edge) {
        links.add(edge);
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Set<Edge> getLinks() {
        return links;
    }
}
