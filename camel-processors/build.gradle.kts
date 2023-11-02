dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.camel.springboot:camel-jetty-starter")
    implementation("org.apache.camel.springboot:camel-http-starter")
    implementation(project(":ids-api")) { isTransitive = false }
    implementation(libs.idscp2.core)
    implementation(libs.camel.idscp2)
    implementation(libs.infomodel)
    implementation(libs.camel.core)
    implementation(libs.guava)
    implementation(libs.apacheHttp.mime)
    implementation(libs.commons.fileupload)

    testImplementation(libs.bundles.camelTest)
}
