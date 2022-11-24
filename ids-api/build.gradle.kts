dependencies {
    implementation(libs.protobuf)
    implementation(libs.jackson.annotations)
    implementation(libs.infomodel.model)
    implementation(libs.infomodel.serializer)
    implementation(libs.camel.core)

    testImplementation(libs.bundles.test4)
}
