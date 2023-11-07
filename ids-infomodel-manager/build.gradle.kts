import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    idea
    alias(libs.plugins.buildconfig)
}

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
        generatedSourceDirs.add(layout.buildDirectory.dir("generated/source/buildConfig/main/main").get().asFile)
    }
}

dependencies {
    implementation(project(":ids-api")) { isTransitive = false }
    implementation(libs.infomodel)
    implementation(libs.commons.cli)
    implementation(libs.javax.validation)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation("org.springframework.boot:spring-boot-starter")
}
