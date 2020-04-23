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

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])
}