@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.ext.get("libraryVersions") as Map<String, String>

dependencies {
    infomodelBundle(project(":ids-api")) { isTransitive = false }

    // Required by MapDB below
    providedByBundle("com.google.guava", "guava", libraryVersions["guava"])
    implementation ("org.mapdb", "mapdb", libraryVersions["mapdb"]) {
        // Exclude guava dependency, which is provided by bundle
        exclude("com.google.guava", "guava")
        exclude("org.jetbrains.kotlin", "*")
    }

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])
}