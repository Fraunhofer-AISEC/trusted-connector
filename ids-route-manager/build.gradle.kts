dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
        rootProject.extra.get("libraryVersions") as Map<String, String>

    implementation(project(":ids-api")) { isTransitive = false }

    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("javax.xml.bind", "jaxb-api", libraryVersions["jaxbApi"])
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])
    implementation("org.apache.camel", "camel-management", libraryVersions["camel"])
    implementation("com.google.guava", "guava", libraryVersions["guava"])

    testImplementation("com.sun.xml.bind", "jaxb-core", libraryVersions["jaxbCore"])
    testImplementation("com.sun.xml.bind", "jaxb-impl", libraryVersions["jaxbImpl"])
    testImplementation("com.sun.activation", "javax.activation", libraryVersions["jaxActivation"])
    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
