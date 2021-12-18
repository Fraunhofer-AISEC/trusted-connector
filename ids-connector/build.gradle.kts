import org.gradle.plugins.ide.idea.model.IdeaModel
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    idea
    application
    alias(libs.plugins.springboot)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.spring)
}

dependencies {
    api(project(":ids-api"))
    // api(project(":ids-acme"))
    api(project(":ids-webconsole"))
    api(project(":ids-settings"))
    api(project(":ids-container-manager"))
    api(project(":ids-route-manager"))
    api(project(":ids-infomodel-manager"))
    api(project(":ids-dataflow-control"))
    api(project(":camel-processors"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Camel Spring Boot integration
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel.springboot:camel-jetty-starter")
    implementation("org.apache.camel.springboot:camel-http-starter")
    implementation("org.apache.camel.springboot:camel-jsonpath-starter")
    // IDSCP2
    implementation(libs.camel.idscp2)
    implementation(libs.idscp2.ra.cmc)
    implementation(libs.grpc.netty)
    // Guava for weakly-keyed Map
    implementation(libs.guava)
}

// Clears library JARs before copying
val cleanLibs = tasks.create<Delete>("deleteLibs") {
    delete(layout.buildDirectory.dir("libs/libraryJars"), layout.buildDirectory.dir("libs/projectJars"))
}
// Copies all runtime library JARs to build/libs/lib
val rootProjectDir: String = rootProject.projectDir.absolutePath
val copyLibraryJars = tasks.create<Copy>("copyLibraryJars") {
    from(
        configurations.runtimeClasspath.get().filterNot {
            it.absolutePath.startsWith(rootProjectDir)
        }
    )
    destinationDir = file(layout.buildDirectory.dir("libs/libraryJars"))
    dependsOn(cleanLibs)
}
val copyProjectJars = tasks.create<Copy>("copyProjectJars") {
    from(
        configurations.runtimeClasspath.get().filter {
            it.absolutePath.startsWith(rootProjectDir)
        }
    )
    destinationDir = file(layout.buildDirectory.dir("libs/projectJars"))
    dependsOn(cleanLibs)
}

tasks.withType<Jar> {
    enabled = true
    // Always execute!
    outputs.upToDateWhen { false }
    dependsOn(copyLibraryJars)
    dependsOn(copyProjectJars)
    // Copy the resulting JAR to internal libraries
    doLast {
        Files.copy(
            Paths.get(archiveFile.get().toString()),
            Paths.get(layout.buildDirectory.file("libs/projectJars/${archiveFileName.get()}").get().toString())
        )
    }
}

// Disable bootJar, as the JAR packaging of Spring Boot prevents dynamic Spring XML parsing due to classpath issues!
tasks.withType<BootJar> {
    enabled = false
}

buildConfig {
    sourceSets.getByName("main") {
        packageName("de.fhg.aisec.ids")
        buildConfigField(
            "String",
            "INFOMODEL_VERSION",
            "\"${libs.versions.infomodel.get()}\""
        )
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(layout.buildDirectory.dir("generated/source/buildConfig/main/main").get().asFile)
    }
}

// Always write project version to version.txt after build/install
tasks.build {
    doLast {
        file(rootProject.projectDir).resolve("version.txt").bufferedWriter().use {
            it.write(rootProject.version.toString())
        }
    }
}
