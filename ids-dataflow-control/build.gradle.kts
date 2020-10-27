description = "Camel IDS Component"

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.ext.get("libraryVersions") as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    providedByBundle("com.google.guava", "guava", libraryVersions["guava"]) {
        isTransitive = false  // Avoid pulling in of checker framework and other annotation stuff
    }

//    providedByBundle("it.unibo.alice.tuprolog", "tuprolog", libraryVersions["tuprolog"])
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
