dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.camel.core)
    implementation(libs.camel.jetty)
    implementation(libs.camel.http)

    implementation(libs.apacheHttp.core)
    implementation(libs.apacheHttp.client)
    implementation(libs.apacheHttp.mime)
    implementation(libs.commons.fileupload)

    testImplementation(libs.bundles.camelTest)
}
