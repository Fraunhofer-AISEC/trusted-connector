description = "Camel IDS Component"

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.ext.get("libraryVersions") as Map<String, String>
    
    providedByBundle(project(":ids-api")) { isTransitive = false }
    providedByBundle(project(":idscp2")) { isTransitive = false }

    // Bill of Materials (BOM) for Camel
    bom("org.apache.camel", "camel-parent", libraryVersions["camel"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])

    compileOnly("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])
    osgiCore("org.osgi", "org.osgi.core", libraryVersions["osgi"])

    providedByFeature("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
