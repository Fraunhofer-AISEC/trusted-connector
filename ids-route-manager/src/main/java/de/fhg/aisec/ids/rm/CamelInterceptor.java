/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
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
package de.fhg.aisec.ids.rm;

import de.fhg.aisec.ids.api.router.RouteManager;
import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.spi.InterceptStrategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(immediate = true, name = "ids-camel-interceptor")
public class CamelInterceptor implements InterceptStrategy {
  @SuppressWarnings("unused")
  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private RouteManager rm;

  @Override
  public Processor wrapProcessorInInterceptors(
      final CamelContext context,
      final NamedNode node,
      final Processor target,
      final Processor nextTarget) {
    return new PolicyEnforcementPoint(node, target, this.rm);
  }
}
