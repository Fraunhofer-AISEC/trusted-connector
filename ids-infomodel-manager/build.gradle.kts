import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    alias(libs.plugins.buildconfig)
}

apply(plugin = "idea")

buildConfig {
    sourceSets.getByName("main") {
        packageName("de.fhg.aisec.ids.informationmodelmanager")
        buildConfigField(
            "String",
            "INFOMODEL_VERSION",
            "\"${libs.versions.infomodel.get()}\""
        )
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(File("$buildDir/generated/source/buildConfig/main/main"))
    }
}

dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    implementation(libs.infomodel.model)
    implementation(libs.infomodel.serializer)
    implementation(libs.commons.cli)
    implementation(libs.javax.validation)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation("org.springframework.boot:spring-boot-starter")
}
