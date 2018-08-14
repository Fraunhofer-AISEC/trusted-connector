/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.ids.comm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.fhg.aisec.ids.api.settings.Settings;
import org.ops4j.peaberry.Peaberry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class InjectionManager {

  private static Injector injector;

  public static void setInjector(Injector injector) {
    InjectionManager.injector = injector;
  }

  public static Injector getInjector() {
    if (injector != null) {
      return injector;
    } else {
      throw new IllegalStateException("Injector has not been set!");
    }
  }

  @Activate
  public void start(BundleContext context) {
    setInjector(
        Guice.createInjector(
            Peaberry.osgiModule(context),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(Settings.class).toProvider(Peaberry.service(Settings.class).single());
              }
            }));
  }
}
