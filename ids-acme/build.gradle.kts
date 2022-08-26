dependencies {
    @Suppress("UNCHECKED_CAST")
    val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

    implementation(project(":ids-api")) { isTransitive = false }

    implementation("org.springframework.boot:spring-boot-starter")

    implementation("org.shredzone.acme4j", "acme4j-client", libraryVersions["acme"])
    implementation("org.shredzone.acme4j", "acme4j-utils", libraryVersions["acme"])
    implementation("org.nanohttpd", "nanohttpd", libraryVersions["nanohttpd"])
}
