package de.fhg.aisec.ids.settings

import de.fhg.aisec.ids.api.settings.ConnectionSettings
import de.fhg.aisec.ids.api.settings.ConnectorConfig
import de.fhg.aisec.ids.api.settings.Settings
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
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
        LOG.info("Open Settings Database...")
        // Use default reliable (non-mmap) mode and WAL for transaction safety
        mapDB = DBMaker.fileDB(DB_PATH.toFile()).transactionEnable().make()
    }

    @Deactivate
    fun deactivate() {
        LOG.info("Close Settings Database...")
        mapDB.close()
    }

    override fun getConnectorConfig(): ConnectorConfig {
        return settingsStore.getOrPut(CONNECTOR_SETTINGS_KEY) { ConnectorConfig() } as ConnectorConfig
    }

    override fun setConnectorConfig(connectorConfig: ConnectorConfig) {
        settingsStore[CONNECTOR_SETTINGS_KEY] = connectorConfig
    }

    override fun getConnectionSettings(connection: String): ConnectionSettings {
        return connectionSettings.getOrPut(connection) { ConnectionSettings() }
    }

    override fun setConnectionSettings(connection: String, conSettings: ConnectionSettings) {
        connectionSettings[connection] = conSettings
    }

    override fun getAllConnectionSettings(): Map<String, ConnectionSettings> {
        return connectionSettings
    }

    companion object {
        internal const val CONNECTOR_SETTINGS_KEY = "main_config"
        internal val DB_PATH = FileSystems.getDefault().getPath("etc", "settings.mapdb")
        private val LOG = LoggerFactory.getLogger(SettingsComponent::class.java)
        private lateinit var mapDB: DB
        private val settingsStore: ConcurrentMap<String, Any> by lazy {
            mapDB.hashMap("settings_store")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(OsgiElsaSerializer<Any>())
                    .createOrOpen()
        }
        private val connectionSettings: ConcurrentMap<String, ConnectionSettings> by lazy {
            mapDB.hashMap("connection_settings")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(OsgiElsaSerializer<ConnectionSettings>())
                    .createOrOpen()
        }
    }
}
