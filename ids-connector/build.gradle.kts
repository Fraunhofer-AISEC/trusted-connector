import org.gradle.plugins.ide.idea.model.IdeaModel
import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    application
    id("org.springframework.boot")
    id("com.github.gmazzo.buildconfig") version "2.0.2"

    kotlin("jvm")
    kotlin("plugin.spring")
}

// Clears library JARs before copying
val cleanLibs = tasks.create<Delete>("deleteLibs") {
    delete("$buildDir/libs/lib")
}
// Copies all runtime library JARs to build/libs/lib
val copyLibs = tasks.create<Copy>("copyLibs") {
    from(configurations.runtimeClasspath)
    destinationDir = file("$buildDir/libs/lib")
    dependsOn(cleanLibs)
}

tasks.withType<Jar> {
    enabled = true
    archiveFileName.set("ids-connector.jar")
    dependsOn(copyLibs)
}

// Disable bootJar, as the JAR packaging of Spring Boot prevents dynamic Spring XML parsing due to classpath issues!
tasks.withType<BootJar> {
    enabled = false
}

apply(plugin = "idea")

buildConfig {
    sourceSets.getByName("main") {
        packageName("de.fhg.aisec.ids")
        buildConfigField(
            "String", "INFOMODEL_VERSION",
            "\"${libraryVersions["infomodel"] ?: error("Infomodel version not available")}\""
        )
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(File("$buildDir/generated/source/buildConfig/main/main"))
    }
}

dependencies {
    api(project(":ids-webconsole"))
    api(project(":ids-settings"))
    api(project(":ids-container-manager"))
    api(project(":ids-route-manager"))
    api(project(":ids-infomodel-manager"))
    api(project(":ids-dataflow-control"))

    // Camel Spring Boot integration
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")

    // Camel components
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel.springboot:camel-http-starter")

    implementation("de.fhg.aisec.ids", "camel-idscp2", libraryVersions["idscp2"])

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-test")
}
