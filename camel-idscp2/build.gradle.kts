@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.ext.get("libraryVersions") as Map<String, String>

version = libraryVersions["idscp2"] ?: error("IDSCP2 version not specified")

dependencies {
    providedByBundle(project(":idscp2-app-layer"))

    publishCompile("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    publishCompile("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodel"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])

    providedByFeature("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])

    providedByBundle("com.google.guava", "guava", libraryVersions["guava"]) {
        isTransitive = false  // Avoid pulling in of checker framework and other annotation stuff
    }

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
