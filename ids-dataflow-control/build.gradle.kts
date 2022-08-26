description = "Camel IDS Component"

dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.infomodel.model)
    implementation(libs.camel.core)
    implementation(libs.guava)
    implementation(libs.bundles.tup)
    implementation(libs.commons.text)
    implementation(libs.protonpack)
    implementation(libs.bundles.ktor.richClient)

    testImplementation(libs.bundles.test4)
}
