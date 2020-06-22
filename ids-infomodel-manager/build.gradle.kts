import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("com.github.gmazzo.buildconfig") version "2.0.2"
}

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

apply(plugin = "idea")

buildConfig {
    sourceSets.getByName("main") {
        packageName("de.fhg.aisec.ids.informationmodelmanager")
        buildConfigField("String", "INFOMODEL_VERSION",
                "\"${libraryVersions["infomodel"] ?: error("Infomodel version not available")}\"")
    }
}

configure<IdeaModel> {
    module {
        // mark as generated sources for IDEA
        generatedSourceDirs.add(File("${buildDir}/generated/source/buildConfig/main/main"))
    }
}

dependencies {
    infomodelBundle(project(":ids-api")) { isTransitive = false }

    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodel"])

    infomodelBundle("commons-cli", "commons-cli", libraryVersions["commonsCli"])

    infomodelBundle("javax.validation", "validation-api", libraryVersions["javaxValidation"])

    infomodelBundle("com.fasterxml.jackson.core", "jackson-annotations", libraryVersions["jackson"])
    infomodelBundle("com.fasterxml.jackson.core", "jackson-databind", libraryVersions["jackson"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    implementation("org.springframework.boot:spring-boot-starter")
}
