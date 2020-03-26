/*-
 * ========================LICENSE_START=================================
 * ids-api
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
package de.fhg.aisec.ids.api.cm;

/**
 * Thrown if a container does not exist.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class NoContainerExistsException extends Exception {
  private static final long serialVersionUID = 3439666843047044252L;

  public NoContainerExistsException(String message) {
    super(message);
  }

  public NoContainerExistsException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
