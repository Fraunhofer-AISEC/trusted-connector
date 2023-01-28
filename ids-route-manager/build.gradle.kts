dependencies {
    implementation(project(":ids-api")) { isTransitive = false }

    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.idscp2.core)
    implementation(libs.camel.idscp2)
    implementation(libs.camel.core)
    implementation(libs.camel.management)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.bundles.jaxbImpl)

    testImplementation(libs.javax.activation)
    testImplementation(libs.bundles.camelTest)
}
