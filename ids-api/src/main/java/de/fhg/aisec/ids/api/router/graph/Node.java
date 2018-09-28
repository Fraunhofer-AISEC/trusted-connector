/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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

import java.util.Objects;

public class Node {
  public enum NodeType {
    EntryNode,
    Node,
    ChoiceNode
  }

  private String name;
  private String action;
  private NodeType type;

  public Node(String name, String action, NodeType type) {
    this.name = name;
    this.action = action;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public String getAction() {
    return action;
  }

  public NodeType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Node)) return false;
    Node node = (Node) o;
    return Objects.equals(name, node.name)
        && Objects.equals(action, node.action)
        && type == node.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, action, type);
  }
}
