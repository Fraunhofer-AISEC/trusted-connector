plugins {
    application
    id("org.springframework.boot")
}

springBoot {
    mainClassName = "de.fhg.aisec.ids.ExampleConnector"
}

dependencies {
    // can and should be replaced by a reference to a maven published artifact later
    implementation(project(":ids-connector"))

    implementation("org.springframework.boot:spring-boot-starter")
}
