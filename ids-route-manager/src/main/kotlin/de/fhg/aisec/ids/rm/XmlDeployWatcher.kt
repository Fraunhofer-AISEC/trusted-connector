package de.fhg.aisec.ids.rm

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
class XmlDeployWatcher {
    private val xmlContexts = mutableMapOf<String, CompletableFuture<AbstractXmlApplicationContext>>()

    private fun startXmlApplicationContext(xmlPath: String) {
        LOG.info("XML file {} detected, creating XmlApplicationContext...", xmlPath)
        val xmlContextFuture: CompletableFuture<AbstractXmlApplicationContext> = CompletableFuture.supplyAsync {
            FileSystemXmlApplicationContext(xmlPath)
        }
        xmlContexts += xmlPath to xmlContextFuture
    }

    private fun stopXmlApplicationContext(xmlPath: String) {
        // If entry is in xmlContexts, remove it stop XmlApplicationContext
        xmlContexts.remove(xmlPath)?.let { ctx ->
            LOG.info("XML file {} deleted, stopping XmlApplicationContext...", xmlPath)
            ctx.thenAccept { it.stop() }
        }
    }

    private fun restartXmlApplicationContext(xmlPath: String) {
        xmlContexts.remove(xmlPath)?.let { ctx ->
            LOG.info("XML file {} modified, restarting XmlApplicationContext...", xmlPath)
            ctx.thenAccept {
                it.stop()
                val xmlContextFuture: CompletableFuture<AbstractXmlApplicationContext> = CompletableFuture.supplyAsync {
                    FileSystemXmlApplicationContext(xmlPath)
                }
                xmlContexts += xmlPath to xmlContextFuture
            }
        }
    }

    init {
        val fs = FileSystems.getDefault()
        val deployPath = fs.getPath("deploy")
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
                        LOG.warn("Watcher stopped by interrupt")
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
                            LOG.error("Error occurred in Deploy Watcher", e)
                        }
                    }
                    // Key must be reset for next iteration, if reset() returns false, key is invalid -> exit
                    if (!key.reset()) {
                        LOG.warn("Watcher stopped by failed reset()")
                        break
                    }
                }
            },
            "XML Deploy Folder Watcher"
        ).run {
            isDaemon = true
            start()
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(XmlDeployWatcher::class.java)
    }
}
