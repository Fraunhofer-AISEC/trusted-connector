// import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    application
    id("org.springframework.boot")
    id("com.github.gmazzo.buildconfig") version "2.0.2"
    // id("com.github.johnrengelman.shadow") version "7.0.0"

    kotlin("jvm")
    kotlin("plugin.spring")
}

tasks.withType<Jar> {
    enabled = true
}

tasks.withType<BootJar> {
    archiveFileName.set("ids-connector.jar")
    layered()
}

configure<JavaApplication> {
    mainClass.set("de.fhg.aisec.ids.TrustedConnector")
}

// tasks.withType<ShadowJar> {
//     archiveFileName.set("ids-connector.jar")
// }

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
}
