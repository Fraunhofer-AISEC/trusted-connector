package de.fhg.camel.ids;

import de.fhg.aisec.ids.api.infomodel.InfoModelManager;
import de.fhg.aisec.ids.api.settings.Settings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component
public class CamelComponent {

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private Settings settings = null;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private InfoModelManager infoModelManager = null;

  private static CamelComponent instance;

  @Activate
  @SuppressWarnings("squid:S2696")
  protected void activate() {
    instance = this;
  }

  @Deactivate
  @SuppressWarnings("squid:S2696")
  protected void deactivate() {
    instance = null;
  }

  @Nullable
  public static Settings getSettings() {
    CamelComponent in = instance;
    if (in != null) {
      return in.settings;
    }
    return null;
  }

  public static InfoModelManager getInfoModelManager() {
    return instance.infoModelManager;
  }


}
