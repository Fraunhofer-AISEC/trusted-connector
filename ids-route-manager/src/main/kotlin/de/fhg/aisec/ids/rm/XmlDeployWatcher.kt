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
import java.nio.file.WatchKey
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.io.path.readBytes

@Component("xmlDeployWatcher")
class XmlDeployWatcher : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    private fun startXmlApplicationContext(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        LOG.info("XML file {} detected, creating XmlApplicationContext...", xmlPathString)
        xmlContexts += xmlPathString to CompletableFuture.supplyAsync {
            FileSystemXmlApplicationContext(arrayOf(xmlPathString), applicationContext)
        }
        xmlHashes += xmlPathString to getPathSha256(xmlPath)
    }

    private fun stopXmlApplicationContext(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        // If entry is in xmlContexts, remove it and stop XmlApplicationContext
        xmlContexts.remove(xmlPathString)?.let { ctxFuture ->
            LOG.info("XML file {} deleted, stopping XmlApplicationContext...", xmlPath)
            xmlHashes -= xmlPathString
            ctxFuture.thenAccept { it.stop() }
        }
    }

    private fun restartXmlApplicationContextIfChanged(xmlPath: Path) {
        val xmlPathString = xmlPath.toString()
        val xmlHash = getPathSha256(xmlPath)
        if (!xmlHash.contentEquals(xmlHashes[xmlPathString])) {
            xmlContexts.remove(xmlPathString)?.let { ctxFuture ->
                LOG.info("XML file {} modified, restarting XmlApplicationContext...", xmlPathString)
                ctxFuture.thenAccept { ctx ->
                    ctx.stop()
                    xmlContexts += xmlPathString to CompletableFuture.supplyAsync {
                        FileSystemXmlApplicationContext(arrayOf(xmlPathString), applicationContext)
                    }
                    xmlHashes += xmlPathString to xmlHash
                }
            }
        } else if (LOG.isDebugEnabled) {
            LOG.debug("XML file {} contents have not changed, skipping reload.", xmlPathString)
        }
    }

    private fun getXmlPathStream(deployPath: Path) = Files.walk(deployPath).filter {
        Files.isRegularFile(it) && it.toString().endsWith(".xml")
    }

    private fun getPathSha256(path: Path) = MessageDigest.getInstance("SHA-256").digest(path.readBytes())

    @Value("\${watcher.delay-ms:10000}")
    private val watcherDelayMs: Long = 10000

    @EventListener(ApplicationReadyEvent::class)
    private fun startXmlDeployWatcher() {
        val fs = FileSystems.getDefault()
        val deployPath = fs.getPath("deploy")
        if (Files.notExists(deployPath)) {
            LOG.info("No deploy folder found, skipping start of XML deploy watcher.")
            return
        }

        // This coroutine performs periodic scanning of the deploy folder.
        // This is especially relevant for Docker containers etc. where inotify doesn't work.
        CoroutineScope(Dispatchers.IO).launch {
            val seenPaths = mutableListOf<String>()
            var lastRunMillis = 0L
            while (true) {
                // Remember the time when we started scanning, since modifications during scan might occur
                val newLastRun = System.currentTimeMillis()
                if (LOG.isTraceEnabled) {
                    LOG.trace("Performing periodic XML watcher scan...")
                }
                try {
                    getXmlPathStream(deployPath).forEach { xmlPath ->
                        val xmlPathString = xmlPath.toString()
                        // Remember handled paths to examine deleted paths later
                        seenPaths += xmlPathString
                        if (xmlPathString !in xmlContexts.keys) {
                            startXmlApplicationContext(xmlPath)
                        } else {
                            // First check file modified time to avoid expensive hashing
                            if (Files.getLastModifiedTime(xmlPath).toMillis() >= lastRunMillis) {
                                restartXmlApplicationContextIfChanged(xmlPath)
                            }
                        }
                    }
                    // Remove paths that vanished since last scan
                    xmlContexts.keys.forEach {
                        if (it !in seenPaths) {
                            stopXmlApplicationContext(Path.of(it))
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
        Thread(
            {
                val watcher = fs.newWatchService()
                deployPath.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
                while (true) {
                    // Get watch key (blocking) to query file events
                    val key: WatchKey = try {
                        watcher.take()
                    } catch (x: InterruptedException) {
                        LOG.warn("XML watcher stopped by interrupt")
                        break
                    }
                    // Remember newly created XML files
                    val createdPaths = mutableSetOf<String>()
                    // Poll the events that happened since last iteration
                    for (watchEvent in key.pollEvents()) {
                        try {
                            val xmlPath = deployPath.resolve(watchEvent.context() as Path)
                            val xmlPathString = xmlPath.toString()
                            when (watchEvent.kind()) {
                                StandardWatchEventKinds.ENTRY_CREATE -> {
                                    createdPaths += xmlPathString
                                    // Must check whether the path represents a valid XML file
                                    if (Files.isRegularFile(xmlPath) || xmlPathString.endsWith(".xml")) {
                                        startXmlApplicationContext(xmlPath)
                                    }
                                }

                                StandardWatchEventKinds.ENTRY_DELETE -> {
                                    stopXmlApplicationContext(xmlPath)
                                }

                                StandardWatchEventKinds.ENTRY_MODIFY -> {
                                    // Ignore MODIFY events for newly created XML files
                                    if (xmlPathString !in createdPaths) {
                                        restartXmlApplicationContextIfChanged(xmlPath)
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
                        break
                    }
                }
            },
            "XML deploy folder watcher"
        ).run {
            isDaemon = true
            start()
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(XmlDeployWatcher::class.java)
        private val xmlContexts = mutableMapOf<String, CompletableFuture<AbstractXmlApplicationContext>>()
        private val xmlHashes = mutableMapOf<String, ByteArray>()

        @Throws(BeansException::class)
        fun <T> getBeansOfType(type: Class<T>?): List<T> {
            return xmlContexts.values
                .filter { it.isDone }
                .flatMap { it.get().getBeansOfType(type).values }
        }
    }
}
