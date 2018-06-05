package de.fhg.aisec.ids.settings

import de.fhg.aisec.ids.api.settings.ConnectorConfig
import de.fhg.aisec.ids.api.settings.Settings
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.mapdb.serializer.GroupSerializer
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.util.concurrent.ConcurrentMap

@Component(immediate = true)
class SettingsComponent : Settings {

    @Activate
    fun activate() {
        LOG.info("Open Settings DB...")
        // Use WAL for transaction safety
        mapDB = DBMaker.fileDB(DB_PATH.toFile()).transactionEnable().make()
        settingsStore = mapDB!!.hashMap("settings_store")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.ELSA as GroupSerializer<Any>)
                .createOrOpen()
    }

    @Deactivate
    fun deactivate() {
        LOG.info("Close Settings DB...")
        mapDB!!.close()
    }

    override fun getConnectorConfig(): ConnectorConfig? {
        return settingsStore!![CONNECTOR_SETTINGS_KEY] as ConnectorConfig?
    }

    override fun setConnectorConfig(connectorConfig: ConnectorConfig) {
        settingsStore!![CONNECTOR_SETTINGS_KEY] = connectorConfig
    }

    companion object {
        internal const val CONNECTOR_SETTINGS_KEY = "main_config"
        internal val DB_PATH = FileSystems.getDefault().getPath("etc", "settings.mapdb")
        private val LOG = LoggerFactory.getLogger(SettingsComponent::class.java)
        private var mapDB: DB? = null
        private var settingsStore: ConcurrentMap<String, Any>? = null
    }
}
