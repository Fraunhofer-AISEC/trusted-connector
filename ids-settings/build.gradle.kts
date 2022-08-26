dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    // Required by MapDB below
    implementation(libs.guava)
    implementation(libs.mapdb) {
        // Exclude guava dependency, which is provided by bundle
        exclude("com.google.guava", "guava")
        exclude("org.jetbrains.kotlin", "*")
    }
    implementation("org.springframework.boot:spring-boot-starter")
}
