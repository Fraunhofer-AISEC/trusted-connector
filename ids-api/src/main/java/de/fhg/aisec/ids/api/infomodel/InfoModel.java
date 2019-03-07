/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
/*
 * Copyright 2018 toni.
 *
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
 */
package de.fhg.aisec.ids.api.infomodel;

import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import java.net.URL;
import java.util.List;

/**
 * @author toni
 */
public interface InfoModel {

  /**
   * retrieve Connector description
   *
   * @return RDF Connector description
   */
  String getRDF();

  /**
   * create empty Connector object
   * @return empty Connector object to be filled
   */
  //Connector getEmptyConnector();

  /**
   * retrieve Connector object based on currently stored preferences
   *
   * @return currently stored Connector object
   */
  Connector getConnector();

  /**
   * save/update Connector object to preferences
   * @param connector filled Connector object
   * @return update success
   */
  //boolean setConnector(Connector connector);

  /**
   * save/update Connector object to preferences
   *
   * @param connUrl Connector id
   * @param opUrl Operator id
   * @param entityNames Connector entity names
   * @param profile Connector security profile
   * @return update success
   */
  boolean setConnector(URL connUrl, URL opUrl, List<PlainLiteral> entityNames,
      SecurityProfile profile);

  /**
   * save/update Connector object to preferences
   *
   * @param connUrl Connector id
   * @param opUrl Operator id
   * @param entityNames Connector entity names
   * @return update success
   */
  boolean setConnector(URL connUrl, URL opUrl, List<PlainLiteral> entityNames);

  /**
   * save/update Connector object to preferences
   *
   * @param opUrl Operator id
   * @param entityNames Connector entity names
   * @return update success
   */
  boolean setConnector(URL opUrl, List<PlainLiteral> entityNames);

}
