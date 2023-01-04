dependencies {
    implementation(project(":ids-api")) { isTransitive = false }

    implementation("org.apache.camel.springboot:camel-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.idscp2.core)
    implementation(libs.camel.idscp2)
    implementation(libs.jaxb.api)
    implementation(libs.camel.core)
    implementation(libs.camel.management)
    implementation(libs.guava)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.bundles.jaxbImpl)
    testImplementation(libs.javax.activation)
    testImplementation(libs.bundles.camelTest)
}
