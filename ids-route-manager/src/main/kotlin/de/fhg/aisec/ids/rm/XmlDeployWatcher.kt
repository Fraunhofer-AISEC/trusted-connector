/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids.rm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.event.EventListener
import org.springframework.context.support.AbstractXmlApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.stereotype.Component
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.io.path.readBytes

@Component("xmlDeployWatcher")
class XmlDeployWatcher : ApplicationContextAware {
    private lateinit var applicationContext: ApplicationContext
    private lateinit var rootBeanRegistry: DefaultListableBeanFactory

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
        rootBeanRegistry = applicationContext.autowireCapableBeanFactory as DefaultListableBeanFactory
    }

    @Synchronized
    private fun startXmlBeans(xmlPathString: String) {
        xmlContexts += xmlPathString to
            CompletableFuture.supplyAsync {
                try {
                    FileSystemXmlApplicationContext(arrayOf(xmlPathString), applicationContext).also { ctx ->
                        // Move special beans prefixed with "root" to the root ApplicationContext
                        (ctx.autowireCapableBeanFactory as DefaultListableBeanFactory).let { registry ->
                            registry.beanDefinitionNames.forEach { LOG.debug("Loaded bean: $it") }
                            registry.beanDefinitionNames
                                .filter { it.startsWith("root") }
                                // Memorize Beans for root ApplicationContext
                                .also { rootBeans += xmlPathString to it }
                                // Move root Beans to root ApplicationContext
                                .forEach { beanName ->
                                    val beanDefinition = registry.getBeanDefinition(beanName)
                                    registry.removeBeanDefinition(beanName)
                                    rootBeanRegistry.registerBeanDefinition(beanName, beanDefinition)
                                    if (LOG.isDebugEnabled) {
                                        LOG.debug("Bean $beanName has been moved to root ApplicationContext")
                                    }
                                }
                        }
                    }
                } catch (t: Throwable) {
                    LOG.error("Error loading $xmlPathString", t)
                    throw t
                }
            }
    }

    @Synchronized
    private fun stopXmlBeans(xmlPathString: String) {
        xmlContexts.remove(xmlPathString)?.let { ctxFuture ->
            ctxFuture.thenAccept { ctx ->
                ctx.stop()
                // Remove moved Beans from root ApplicationContext
                rootBeans.remove(xmlPathString)?.forEach {
                    rootBeanRegistry.removeBeanDefinition(it)
                }
            }
        } ?: LOG.warn("ApplicationContext for $xmlPathString not found, cannot stop it.")
    }

    private fun loadXml(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        LOG.info("XML file {} detected, loading...", xmlPathString)
        startXmlBeans(xmlPathString)
        xmlHashes += xmlPathString to getPathSha256(xmlPath)
    }

    private fun unloadXml(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        // If entry is in xmlContexts, remove it and stop XmlApplicationContext
        LOG.info("XML file {} deleted, stopping...", xmlPathString)
        stopXmlBeans(xmlPathString)
        xmlHashes -= xmlPathString
    }

    private fun reloadXml(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        val xmlHash = getPathSha256(xmlPath)
        if (!xmlHash.contentEquals(xmlHashes[xmlPathString])) {
            LOG.info("XML file {} modified, restarting...", xmlPathString)
            stopXmlBeans(xmlPathString)
            startXmlBeans(xmlPathString)
            xmlHashes += xmlPathString to xmlHash
        } else if (LOG.isDebugEnabled) {
            LOG.debug("XML file {} contents have not changed, skipping reload.", xmlPathString)
        }
    }

    private fun getXmlPathStream() =
        Files.walk(DEPLOY_PATH).filter {
            Files.isRegularFile(it) && it.toString().endsWith(".xml")
        }

    private fun getPathSha256(path: Path) = MessageDigest.getInstance("SHA-256").digest(path.readBytes())

    @Value("\${watcher.delay-ms:10000}")
    private val watcherDelayMs: Long = 10000

    @EventListener(ApplicationReadyEvent::class)
    fun startXmlDeployWatcher() {
        if (Files.notExists(DEPLOY_PATH)) {
            LOG.info("No deploy folder found, skipping launch of XML deploy watcher.")
            return
        }

        // This coroutine performs periodic scanning of the deploy folder.
        // This is especially relevant for Docker containers etc. where inotify doesn't work.
        IO_SCOPE.launch {
            val seenPaths = mutableListOf<String>()
            var lastRunMillis = 0L
            while (true) {
                // Remember the time when we started scanning, since modifications during scan might occur
                val newLastRun = System.currentTimeMillis()
                if (LOG.isTraceEnabled) {
                    LOG.trace("Performing periodic XML watcher scan...")
                }
                try {
                    getXmlPathStream().forEach { xmlPath ->
                        val xmlPathString = xmlPath.toString()
                        // Remember handled paths to examine deleted paths later
                        seenPaths += xmlPathString
                        if (xmlPathString !in xmlContexts.keys) {
                            loadXml(xmlPath)
                        } else {
                            // First check file modified time to avoid expensive hashing
                            if (Files.getLastModifiedTime(xmlPath).toMillis() >= lastRunMillis) {
                                reloadXml(xmlPath)
                            }
                        }
                    }
                    // Remove paths that vanished since last scan
                    synchronized(this@XmlDeployWatcher) {
                        xmlContexts.keys.forEach {
                            if (it !in seenPaths) {
                                unloadXml(Path.of(it))
                            }
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Error during periodic XML watcher scan", e)
                }
                seenPaths.clear()
                lastRunMillis = newLastRun
                delay(watcherDelayMs)
            }
        }

        // WatcherService that may react faster to changes in the deploy folder.
        // Known not to work in some containerized environments.
        IO_SCOPE.launch {
            val watcher = FS.newWatchService()
            DEPLOY_PATH.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
            while (true) {
                // Poll the events that happened since last iteration
                watcher.poll()?.let { key ->
                    // Remember newly created XML files
                    val createdPaths = mutableSetOf<String>()
                    key.pollEvents().forEach { watchEvent ->
                        try {
                            val xmlPath = DEPLOY_PATH.resolve(watchEvent.context() as Path)
                            val xmlPathString = xmlPath.toString()
                            when (watchEvent.kind()) {
                                StandardWatchEventKinds.ENTRY_CREATE -> {
                                    createdPaths += xmlPathString
                                    // Must check whether the path represents a valid XML file
                                    if (Files.isRegularFile(xmlPath) || xmlPathString.endsWith(".xml")) {
                                        loadXml(xmlPath)
                                    }
                                }

                                StandardWatchEventKinds.ENTRY_DELETE -> {
                                    unloadXml(xmlPath)
                                }

                                StandardWatchEventKinds.ENTRY_MODIFY -> {
                                    // Ignore MODIFY events for newly created XML files
                                    if (xmlPathString !in createdPaths) {
                                        reloadXml(xmlPath)
                                    }
                                }

                                else -> {
                                    LOG.warn("Unhandled WatchEvent: {}", watchEvent)
                                }
                            }
                        } catch (e: Exception) {
                            LOG.error("Error occurred in deploy watcher", e)
                        }
                    }
                    // Key must be reset for next iteration, if reset() returns false, key is invalid -> exit
                    if (!key.reset()) {
                        LOG.warn("XML watcher stopped by failed reset()")
                        return@launch
                    }
                }
                delay(FS_WATCHER_POLL_INTERVAL)
            }
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(XmlDeployWatcher::class.java)
        private val xmlContexts = mutableMapOf<String, CompletableFuture<AbstractXmlApplicationContext>>()
        private val rootBeans = mutableMapOf<String, List<String>>()
        private val xmlHashes = mutableMapOf<String, ByteArray>()
        private val FS = FileSystems.getDefault()
        private val DEPLOY_PATH = FS.getPath("deploy")

        // CoroutineScope using IO Dispatcher
        private val IO_SCOPE = CoroutineScope(Dispatchers.IO)

        // Selector Key polling interval for native Watcher
        private const val FS_WATCHER_POLL_INTERVAL = 1000L

        @Throws(BeansException::class)
        fun <T> getBeansOfType(type: Class<T>?): List<T> =
            xmlContexts.values
                .filter { it.isDone }
                .flatMap { it.get().getBeansOfType(type).values }
    }
}
