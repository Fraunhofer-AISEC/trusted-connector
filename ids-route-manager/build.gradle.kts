dependencies {
    implementation(project(":ids-api")) { isTransitive = false }

    implementation(libs.camel.idscp2)
    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.jaxb.api)
    implementation(libs.camel.core)
    implementation(libs.camel.management)
    implementation(libs.guava)

    testImplementation(libs.bundles.jaxbImpl)
    testImplementation(libs.javax.activation)
    testImplementation(libs.bundles.camelTest)
}
