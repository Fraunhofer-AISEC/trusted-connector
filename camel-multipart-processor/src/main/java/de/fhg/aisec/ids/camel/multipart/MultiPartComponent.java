/*-
 * ========================LICENSE_START=================================
 * camel-multipart-processor
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
package de.fhg.aisec.ids.camel.multipart;

import de.fhg.aisec.ids.api.infomodel.InfoModel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * The only purpose of this OSGi component is to connect to the InfoModelManager.
 *
 * <p>This is required for the MultiPartComponent to use a proper IDS self description in the
 * multipart messages.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 */
@Component(name = "ids-multipart-component")
public class MultiPartComponent {

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private InfoModel infoModel = null;

  private static MultiPartComponent instance;

  @Activate
  @SuppressWarnings("squid:S2696")
  protected void activate() {
    instance = this;
  }

  public static InfoModel getInfoModelManager() {
    return instance.infoModel;
  }
}
