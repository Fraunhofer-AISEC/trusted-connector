dependencies {
    implementation(project(":ids-api")) { isTransitive = false }

    implementation("org.springframework.boot:spring-boot-starter")

    implementation(libs.bundles.acme4jFull)
    implementation(libs.nanohttpd)
}
