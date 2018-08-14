/*-
 * ========================LICENSE_START=================================
 * ids-dataflow-control
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
package de.fhg.ids.dataflowcontrol;

import static de.fhg.ids.dataflowcontrol.lucon.TuPrologHelper.escape;
import static de.fhg.ids.dataflowcontrol.lucon.TuPrologHelper.listStream;

import alice.tuprolog.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.fhg.aisec.ids.api.policy.*;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteVerificationProof;
import de.fhg.ids.dataflowcontrol.lucon.LuconEngine;
import de.fhg.ids.dataflowcontrol.lucon.TuPrologHelper;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * servicefactory=false is the default and actually not required. But we want to make clear that
 * this is a singleton, i.e. there will only be one instance of PolicyDecisionPoint within the whole
 * runtime.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-dataflow-control")
public class PolicyDecisionPoint implements PDP, PAP {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyDecisionPoint.class);
  private static final String LUCON_FILE_EXTENSION = ".pl";

  @NonNull private LuconEngine engine = new LuconEngine(System.out);
  @Nullable private RouteManager routeManager;
  private Cache<ServiceNode, TransformationDecision> transformationCache =
      CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(1, TimeUnit.DAYS).build();

  /**
   * Creates a query to retrieve policy decision from Prolog knowledge base.
   *
   * <p>Result of query will be:
   *
   * <p>Target Decision Alternative Obligation hadoopClusters D drop
   * (delete_after_days(_1354),_1354<30) 1 hiveMqttBroker drop Alt A
   *
   * @param target The target node of the transformation
   * @param msgLabels The dynamically assigned message labels
   */
  private String createDecisionQuery(
      @NonNull ServiceNode target, @NonNull Map<String, Object> msgLabels) {
    StringBuilder sb = new StringBuilder();
    sb.append("rule(X), has_target(X, T), ");
    sb.append("has_endpoint(T, EP), ");
    sb.append("regex_match(EP, ").append(escape(target.getEndpoint())).append("), ");
    msgLabels
        .keySet()
        .stream()
        .filter(
            k ->
                k.startsWith(LABEL_PREFIX)
                    && msgLabels.get(k) != null
                    && !msgLabels.get(k).toString().equals(""))
        .forEach(
            k -> {
              // QUERY structure must be: has_endpoint(rule, bla), assert(label(x)),
              // receives_label(rule).
              sb.append("assert(label(")
                  .append(TuPrologHelper.escape(msgLabels.get(k).toString()))
                  .append(")), ");
            });
    sb.append("receives_label(X), ");
    sb.append("rule_priority(X, P), ");
    if (target.getCapabilties().size() + target.getProperties().size() > 0) {
      List<String> capProp = new LinkedList<>();
      for (String cap : target.getCapabilties()) {
        capProp.add("has_capability(T, " + escape(cap) + ")");
      }
      for (String prop : target.getProperties()) {
        capProp.add("has_property(T, " + escape(prop) + ")");
      }
      sb.append("(").append(capProp.stream().collect(Collectors.joining(", "))).append("), ");
    }
    sb.append(
        "(has_decision(X, D); (has_obligation(X, _O), has_alternativedecision(_O, Alt), "
            + "requires_prerequisite(_O, A))).");
    return sb.toString();
  }

  /**
   * A transformation query retrieves the set of labels to add and to remove from the Prolog
   * knowledge base.
   *
   * <p>This method returns the respective query for a specific target.
   *
   * @param target The ServiceNode to be processed
   * @return The resulting Prolog query for the transformation
   */
  private String createTransformationQuery(@NonNull ServiceNode target) {
    StringBuilder sb = new StringBuilder();
    if (target.getEndpoint() != null) {
      sb.append("dominant_allow_rules(").append(escape(target.getEndpoint())).append(", _T, _), ");
    } else {
      throw new RuntimeException("No endpoint specified!");
    }
    if (target.getCapabilties().size() + target.getProperties().size() > 0) {
      List<String> capProp = new LinkedList<>();
      for (String cap : target.getCapabilties()) {
        capProp.add("has_capability(_T, " + escape(cap) + ")");
      }
      for (String prop : target.getProperties()) {
        capProp.add("has_property(_T, " + escape(prop) + ")");
      }
      sb.append('(').append(capProp.stream().collect(Collectors.joining(", "))).append("),\n");
    }
    sb.append("once(setof(S, action_service(")
        .append(escape(target.getEndpoint()))
        .append(", S), SC); SC = []),\n")
        .append("collect_creates_labels(SC, ACraw), set_of(ACraw, Adds),\n")
        .append("collect_removes_labels(SC, RCraw), set_of(RCraw, Removes).");
    return sb.toString();
  }

  @Activate
  protected void activate(@NonNull ComponentContext ctx) throws IOException {
    loadPolicies();
  }

  @Reference(
    name = "pdp-routemanager",
    policy = ReferencePolicy.DYNAMIC,
    cardinality = ReferenceCardinality.OPTIONAL
  )
  protected void bindRouteManager(@NonNull RouteManager routeManager) {
    LOG.debug("RouteManager bound. Camel routes can be analyzed");
    this.routeManager = routeManager;
  }

  @SuppressWarnings("unused")
  protected void unbindRouteManager(@NonNull RouteManager routeManager) {
    LOG.debug(
        "RouteManager unbound. Will not be able to verify Camel routes against policies anymore");
    this.routeManager = null;
  }

  protected void loadPolicies() throws FileNotFoundException {
    // Try to load existing policies from deploy dir at activation
    File dir = new File(System.getProperty("karaf.base") + File.separator + "deploy");
    File[] directoryListing = dir.listFiles();
    if (directoryListing == null || !dir.isDirectory()) {
      LOG.warn("Unexpected or not running in karaf: Not a directory: " + dir.getAbsolutePath());
      return;
    }

    boolean loaded = false;
    for (File f : directoryListing) {
      if (f.getName().endsWith(LUCON_FILE_EXTENSION)) {
        if (!loaded) {
          LOG.info("Loading Lucon policy from " + f.getAbsolutePath());
          loadPolicy(new FileInputStream(f));
          loaded = true;
        } else {
          LOG.warn("Multiple policy files. Will load only one! " + f.getAbsolutePath());
        }
      }
    }
  }

  @Override
  public TransformationDecision requestTranformations(@Nullable ServiceNode lastServiceNode) {
    if (lastServiceNode == null) {
      return new TransformationDecision();
    }
    try {
      return transformationCache.get(
          lastServiceNode,
          () -> {
            // Query prolog for labels to remove or add from message
            String query = this.createTransformationQuery(lastServiceNode);
            LOG.info("QUERY: " + query);

            TransformationDecision result = new TransformationDecision();
            try {
              List<SolveInfo> solveInfo = this.engine.query(query, true);
              if (solveInfo.isEmpty()) {
                return result;
              }

              // Get solutions, convert label variables to string and collect in sets
              Set<String> labelsToAdd = result.getLabelsToAdd();
              Set<String> labelsToRemove = result.getLabelsToRemove();
              solveInfo.forEach(
                  s -> {
                    try {
                      Term adds = s.getVarValue("Adds").getTerm();
                      if (adds.isList()) {
                        listStream(adds).map(Term::toString).forEach(labelsToAdd::add);
                      } else {
                        throw new RuntimeException("\"Adds\" is not a prolog list!");
                      }
                      Term removes = s.getVarValue("Removes").getTerm();
                      if (removes.isList()) {
                        listStream(removes).map(Term::toString).forEach(labelsToRemove::add);
                      } else {
                        throw new RuntimeException("\"Removes\" is not a prolog list!");
                      }
                    } catch (NoSolutionException ignored) {
                    }
                  });
            } catch (NoMoreSolutionException | MalformedGoalException e) {
              LOG.error(e.getMessage(), e);
            }
            return result;
          });
    } catch (ExecutionException ee) {
      LOG.error(ee.getMessage(), ee);
      return new TransformationDecision();
    }
  }

  @Override
  public PolicyDecision requestDecision(@Nullable DecisionRequest req) {
    PolicyDecision dec = new PolicyDecision();
    dec.setDecision(Decision.DENY); // Default value
    if (req == null) {
      dec.setReason("Null request");
      return dec;
    }
    LOG.debug(
        "Decision requested " + req.getFrom().getEndpoint() + " -> " + req.getTo().getEndpoint());

    try {
      // Query Prolog engine for a policy decision
      long startTime = System.nanoTime();
      String query = this.createDecisionQuery(req.getTo(), req.getMessageCtx());
      LOG.info("QUERY: " + query);
      List<SolveInfo> solveInfo = this.engine.query(query, true);
      long time = System.nanoTime() - startTime;
      LOG.info("Policy decision took " + time + " nanos");

      // If there is no matching rule, allow by default
      if (solveInfo.isEmpty()) {
        LOG.trace("No policy decision found. Returning " + dec.getDecision().toString());
        dec.setReason("No matching rule");
        return dec;
      }

      // Include only solveInfos with highest priority
      int maxPrio = Integer.MIN_VALUE;
      for (SolveInfo si : solveInfo) {
        try {
          int priority = Integer.parseInt(si.getVarValue("P").getTerm().toString());
          if (priority > maxPrio) {
            maxPrio = priority;
          }
        } catch (NumberFormatException | NullPointerException e) {
          LOG.warn("Invalid rule priority: " + si.getVarValue("P"), e);
        }
      }
      List<SolveInfo> applicableSolveInfos = new ArrayList<>();
      for (SolveInfo si : solveInfo) {
        try {
          int priority = Integer.parseInt(si.getVarValue("P").getTerm().toString());
          if (priority == maxPrio) {
            applicableSolveInfos.add(si);
          }
        } catch (NumberFormatException | NullPointerException e) {
          LOG.warn("Invalid rule priority: " + si.getVarValue("P"), e);
        }
      }

      // Just for debugging
      if (LOG.isDebugEnabled()) {
        debug(applicableSolveInfos);
      }

      // Collect obligations
      List<Obligation> obligations = new LinkedList<>();
      applicableSolveInfos.forEach(
          s -> {
            try {
              Term rule = s.getVarValue("X");
              Term decision = s.getVarValue("D");
              if (!(decision instanceof Var)) {
                String decString = decision.getTerm().toString();
                if ("drop".equals(decString)) {
                  dec.setReason(rule.getTerm().toString());
                  dec.setDecision(Decision.DENY);
                } else if ("allow".equals(decString)) {
                  dec.setReason(rule.getTerm().toString());
                  dec.setDecision(Decision.ALLOW);
                }
              }
              Term action = s.getVarValue("A"), altDecision = s.getVarValue("Alt");
              if (!(action instanceof Var)) {
                Obligation o = new Obligation();
                o.setAction(action.getTerm().toString());
                if (!(altDecision instanceof Var)) {
                  String altDecString = altDecision.getTerm().toString();
                  if ("drop".equals(altDecString)) {
                    o.setAlternativeDecision(Decision.DENY);
                  } else if ("allow".equals(altDecString)) {
                    o.setAlternativeDecision(Decision.ALLOW);
                  }
                }
                obligations.add(o);
              }
            } catch (NoSolutionException e) {
              LOG.warn("Unexpected: solution variable not present: " + e.getMessage());
              dec.setReason("Solution variable not present");
            }
          });
      dec.setObligations(obligations);
    } catch (NoMoreSolutionException | MalformedGoalException | NoSolutionException e) {
      LOG.error(e.getMessage(), e);
      dec.setReason("Error " + e.getMessage());
      dec.setDecision(Decision.DENY);
    }
    return dec;
  }

  /**
   * Just for debugging: Print query solution to DEBUG out.
   *
   * @param solveInfo A list of Prolog solutions
   */
  private void debug(@NonNull List<SolveInfo> solveInfo) {
    if (!LOG.isTraceEnabled()) {
      return;
    }
    try {
      for (SolveInfo i : solveInfo) {
        if (i.isSuccess()) {
          List<Var> vars = i.getBindingVars();
          vars.forEach(v -> LOG.trace(v.getName() + ":" + v.getTerm() + " bound: " + v.isBound()));
        }
      }
    } catch (NoSolutionException nse) {
      LOG.trace("No solution found", nse);
    }
  }

  @Override
  public void clearAllCaches() {
    // clear Prolog cache entries
    try {
      engine.query("cache_clear(_).", true);
      LOG.info("Prolog cache_clear(_) successful");
    } catch (PrologException pe) {
      LOG.warn("Prolog cache_clear(_) failed", pe);
    }
    // clear transformation cache
    transformationCache.invalidateAll();
  }

  @Override
  public void loadPolicy(@Nullable InputStream is) {
    try {
      // Load policy into engine, possibly overwriting the existing one.
      this.engine.loadPolicy(is);
    } catch (InvalidTheoryException e) {
      LOG.error("Error in " + e.line + " " + e.pos + ": " + e.clause + ": " + e.getMessage(), e);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @Override
  public List<String> listRules() {
    ArrayList<String> result = new ArrayList<>();
    try {
      List<SolveInfo> rules = this.engine.query("rule(X).", true);
      for (SolveInfo r : rules) {
        result.add(r.getVarValue("X").toString());
      }
    } catch (PrologException e) {
      LOG.error("Prolog error while retrieving rules " + e.getMessage(), e);
    }
    return result;
  }

  @Override
  public String getPolicy() {
    return this.engine.getTheory();
  }

  @Override
  public RouteVerificationProof verifyRoute(String routeId) {
    RouteManager rm = this.routeManager;
    if (rm == null) {
      LOG.warn("No RouteManager. Cannot verify Camel route " + routeId);
      return null;
    }

    String routePl = rm.getRouteAsProlog(routeId);
    if (routePl == null) {
      LOG.warn("Could not obtain Prolog representation of route " + routeId);
      return null;
    }

    return engine.proofInvalidRoute(routeId, routePl);
  }
}
