plugins {
    application
    id("org.springframework.boot")
}

springBoot {
    mainClass.set("de.fhg.aisec.ids.ExampleConnector")
}

dependencies {
    // Can and should be replaced by a reference to a maven published artifact later
    implementation(project(":ids-connector"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
}
