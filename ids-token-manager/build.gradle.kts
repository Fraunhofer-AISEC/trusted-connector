@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.ext.get("libraryVersions") as Map<String, String>

dependencies {
    providedByBundle(project(":ids-api")) { isTransitive = false }

    implementation("de.fhg.aisec.ids", "idscp2", libraryVersions["idscp2"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
