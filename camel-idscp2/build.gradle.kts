description = "Camel IDS Component"

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.ext.get("libraryVersions") as Map<String, String>
    
    providedByBundle(project(":ids-api")) { isTransitive = false }
    providedByBundle(project(":idscp2-app-layer"))

    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodelSerializer"])

    // Bill of Materials (BOM) for Camel
    bom("org.apache.camel", "camel-parent", libraryVersions["camel"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    providedByFeature("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
