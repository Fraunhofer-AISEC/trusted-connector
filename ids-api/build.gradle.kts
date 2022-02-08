@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation("com.google.protobuf", "protobuf-java", libraryVersions["protobuf"])
    implementation("com.fasterxml.jackson.core", "jackson-annotations", libraryVersions["jackson"])
    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])

    compileOnly("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
}
