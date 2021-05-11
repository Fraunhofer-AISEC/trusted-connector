@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    // Required by MapDB below
    implementation("com.google.guava", "guava", libraryVersions["guava"])
    implementation("org.mapdb", "mapdb", libraryVersions["mapdb"]) {
        // Exclude guava dependency, which is provided by bundle
        exclude("com.google.guava", "guava")
        exclude("org.jetbrains.kotlin", "*")
    }
    implementation("org.springframework.boot:spring-boot-starter")
}
