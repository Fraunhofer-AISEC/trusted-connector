import org.yaml.snakeyaml.Yaml

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            Yaml().load(File("${rootDir}/libraryVersions.yaml").inputStream()) as Map<String, String>

    infomodelBundle(project(":ids-api")) { isTransitive = false }

    // Required by MapDB below
    providedByBundle("com.google.guava", "guava", libraryVersions["guava"]) {
        isTransitive = false  // Avoid pulling in of checker framework and other annotation stuff
    }
    implementation ("org.mapdb", "mapdb", libraryVersions["mapdb"]) {
        // Exclude guava dependency, which is provided by bundle
        exclude("com.google.guava", "guava")
        exclude("org.jetbrains.kotlin", "*")
    }

    providedByBundle("org.apache.logging.log4j", "log4j-core", libraryVersions["log4j"])
    providedByBundle("org.apache.logging.log4j", "log4j-slf4j-impl", libraryVersions["log4j"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])
}