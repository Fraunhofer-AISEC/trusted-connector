description = "Camel IDS Component"

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.extra.get("libraryVersions") as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    implementation("org.springframework.boot:spring-boot-starter")

    providedByBundle("com.google.guava", "guava", libraryVersions["guava"])

    implementation("it.unibo.alice.tuprolog", "2p-core", libraryVersions["2p"])
    implementation("it.unibo.alice.tuprolog", "2p-parser", libraryVersions["2p"])
//    implementation("it.unibo.alice.tuprolog", "2p-presentation", libraryVersions["2p"])
    providedByBundle("org.apache.commons", "commons-text", libraryVersions["commonsText"])
    providedByBundle("com.codepoetics", "protonpack", libraryVersions["protonpack"])
    providedByBundle("org.antlr", "antlr4-runtime", libraryVersions["antlr4"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
