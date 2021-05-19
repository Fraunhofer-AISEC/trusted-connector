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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
import java.util.concurrent.CompletableFuture

@Component("xmlDeployWatcher")
class XmlDeployWatcher : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    private val xmlContexts = mutableMapOf<String, CompletableFuture<AbstractXmlApplicationContext>>()

    private fun startXmlApplicationContext(xmlPath: String) {
        LOG.info("XML file {} detected, creating XmlApplicationContext...", xmlPath)
        val xmlContextFuture: CompletableFuture<AbstractXmlApplicationContext> = CompletableFuture.supplyAsync {
            FileSystemXmlApplicationContext(arrayOf(xmlPath), applicationContext)
        }
        xmlContexts += xmlPath to xmlContextFuture
    }

    private fun stopXmlApplicationContext(xmlPath: String) {
        // If entry is in xmlContexts, remove it stop XmlApplicationContext
        xmlContexts.remove(xmlPath)?.let { ctxFuture ->
            LOG.info("XML file {} deleted, stopping XmlApplicationContext...", xmlPath)
            ctxFuture.thenAccept { it.stop() }
        }
    }

    private fun restartXmlApplicationContext(xmlPath: String) {
        xmlContexts.remove(xmlPath)?.let { ctxFuture ->
            LOG.info("XML file {} modified, restarting XmlApplicationContext...", xmlPath)
            ctxFuture.thenAccept { ctx ->
                ctx.stop()
                val xmlContextFuture: CompletableFuture<AbstractXmlApplicationContext> = CompletableFuture.supplyAsync {
                    FileSystemXmlApplicationContext(arrayOf(xmlPath), applicationContext)
                }
                xmlContexts += xmlPath to xmlContextFuture
            }
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    private fun startXmlDeployWatcher() {
        val fs = FileSystems.getDefault()
        val deployPath = fs.getPath("deploy")
        if (Files.notExists(deployPath)) {
            LOG.info("No deploy folder found, skipping start of XML deploy watcher.")
            return
        }
        Files.walk(deployPath)
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".xml") }
            .forEach { startXmlApplicationContext(it.toString()) }
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
                    // Poll the events that happened since last iteration
                    for (watchEvent in key.pollEvents()) {
                        try {
                            val xmlPath = deployPath.resolve(watchEvent.context() as Path)
                            val xmlPathString = xmlPath.toString()
                            when (watchEvent.kind()) {
                                StandardWatchEventKinds.ENTRY_CREATE -> {
                                    // Must check whether the path represents a valid XML file
                                    if (Files.isRegularFile(xmlPath) || xmlPathString.endsWith(".xml")) {
                                        startXmlApplicationContext(xmlPathString)
                                    }
                                }
                                StandardWatchEventKinds.ENTRY_DELETE -> {
                                    stopXmlApplicationContext(xmlPathString)
                                }
                                StandardWatchEventKinds.ENTRY_MODIFY -> {
                                    restartXmlApplicationContext(xmlPathString)
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
        val LOG: Logger = LoggerFactory.getLogger(XmlDeployWatcher::class.java)
    }
}
