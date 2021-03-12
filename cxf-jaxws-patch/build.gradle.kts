@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation("javax.xml.ws", "jaxws-api", libraryVersions["jaxwsApi"])
}
