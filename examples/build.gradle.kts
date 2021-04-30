import org.gradle.kotlin.dsl.support.zipTo

// Copy example files to build directory
val copyExample = tasks.create<Copy>("copyExample") {
    // Delete the build directory first to start with a clean state
    doFirst {
        delete(project.buildDir)
    }
    from(project.projectDir) {
        include("example-*/**", "tpmsim/tpmsim_data.tar", "tpmsim/rootCA.crt", "cert-stores/*")
        exclude("example-idscp/example-client", "example-idscp/example-server")
    }
    into(project.buildDir)
}

// Overwrite .env files with evaluated templates to use given docker tag
val processTemplates = tasks.create<Copy>("processTemplates") {
    from("templates")
    include("**/.env")
    into(project.buildDir)
    expand(Pair("exampleTag", findProperty("exampleTag")))
    inputs.property("exampleTag", findProperty("exampleTag"))
    inputs.dir("templates").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.cacheIf { true }
}
processTemplates.dependsOn(copyExample)
processTemplates.onlyIf { findProperty("exampleTag") != null }

// Create ZIP archive from examples, see task copyExample
val zipExample = task("zipExample") {
    doLast {
        zipTo(
            File("$projectDir/trusted-connector-examples_${findProperty("exampleTag")}.zip"),
            project.buildDir
        )
    }
}
zipExample.onlyIf { findProperty("exampleTag") != null }
zipExample.dependsOn(processTemplates)
tasks.processResources.configure { dependsOn(zipExample) }

// Remove build directory after all tasks have been executed
val buildDir: File = project.buildDir.absoluteFile
gradle.taskGraph.whenReady {
    val remainingTasks = HashSet(gradle.taskGraph.allTasks.filter { it.path.startsWith(":examples") })
    remainingTasks.forEach { t ->
        t.doLast {
            remainingTasks.filter { it.state.skipped }.forEach {
                remainingTasks.remove(it)
            }
            remainingTasks.remove(t)
            if (remainingTasks.isEmpty()) {
                delete(buildDir)
            }
        }
    }
}
