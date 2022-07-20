description = "Camel IDS Component"

dependencies {
    @Suppress("UNCHECKED_CAST")
    val libraryVersions =
        rootProject.extra.get("libraryVersions") as Map<String, String>

    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])
    implementation("com.google.guava", "guava", libraryVersions["guava"])
    implementation("it.unibo.alice.tuprolog", "2p-core", libraryVersions["2p"])
    implementation("it.unibo.alice.tuprolog", "2p-parser", libraryVersions["2p"])
//    implementation("it.unibo.alice.tuprolog", "2p-presentation", libraryVersions["2p"])
    implementation("org.apache.commons", "commons-text", libraryVersions["commonsText"])
    implementation("com.codepoetics", "protonpack", libraryVersions["protonpack"])
    implementation("org.antlr", "antlr4-runtime", libraryVersions["antlr4"])
    implementation("io.ktor", "ktor-client-core", libraryVersions["ktor"])
    implementation("io.ktor", "ktor-client-java", libraryVersions["ktor"])
    implementation("io.ktor", "ktor-client-content-negotiation", libraryVersions["ktor"])
    implementation("io.ktor", "ktor-serialization-jackson", libraryVersions["ktor"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
