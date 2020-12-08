dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.ext.get("libraryVersions") as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodel"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])
    providedByFeature("org.apache.camel", "camel-management", libraryVersions["camel"])

    osgiCore("org.apache.felix", "org.apache.felix.framework", libraryVersions["felixFramework"])
    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    providedByBundle("com.google.guava", "guava", libraryVersions["guava"]) {
        isTransitive = false  // Avoid pulling in of checker framework and other annotation stuff
    }

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
