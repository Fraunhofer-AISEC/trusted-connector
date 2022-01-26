dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
        rootProject.extra.get("libraryVersions") as Map<String, String>

    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.camel", "camel-core", libraryVersions["camel"])
    implementation("org.apache.camel", "camel-jetty", libraryVersions["camel"])
    implementation("org.apache.camel", "camel-http", libraryVersions["camel"])

    implementation("org.apache.httpcomponents", "httpcore", libraryVersions["httpcore"])
    implementation("org.apache.httpcomponents", "httpclient", libraryVersions["httpclient"])
    implementation("org.apache.httpcomponents", "httpmime", libraryVersions["httpclient"])
    implementation("commons-fileupload", "commons-fileupload", libraryVersions["commonsFileUpload"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
