dependencies {
    implementation("org.springframework.boot:spring-boot-starter")

    implementation(project(":ids-api")) { isTransitive = false }

    implementation(libs.idscp2.core)
    implementation(libs.camel.idscp2)

    implementation(libs.infomodel.model)
    implementation(libs.infomodel.serializer)

    implementation(libs.camel.core)

    implementation(libs.guava)

    testImplementation(libs.bundles.camelTest)
}
