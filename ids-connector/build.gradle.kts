import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    application
    id("org.springframework.boot")

    kotlin("jvm")
    kotlin("plugin.spring")
}

tasks.getByName<BootJar>("bootJar") {
    launchScript()
    layered()
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

    implementation("de.fhg.aisec.ids", "camel-idscp2", libraryVersions["idscp2"]) {
        exclude("org.slf4j", "slf4j-simple") // needed until https://github.com/industrial-data-space/idscp2-java/pull/4 is merged
    }

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
}
