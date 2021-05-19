dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
        rootProject.extra.get("libraryVersions") as Map<String, String>

    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])
    implementation("org.apache.camel", "camel-http4", libraryVersions["camelHttp4"])
    implementation("org.apache.httpcomponents", "httpcore-osgi", libraryVersions["httpcore"])
    implementation("org.apache.httpcomponents", "httpclient-osgi", libraryVersions["httpclient"])
    implementation("commons-fileupload", "commons-fileupload", libraryVersions["commonsFileUpload"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
