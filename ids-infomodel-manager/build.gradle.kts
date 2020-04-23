import org.yaml.snakeyaml.Yaml

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            Yaml().load(File("${rootDir}/libraryVersions.yaml").inputStream()) as Map<String, String>

    infomodelBundle(project(":ids-api")) { isTransitive = false }

    // MVN resolution problem workaround for infomodel until publicly released
    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodelSerializer"])

    infomodelBundle("commons-cli", "commons-cli", libraryVersions["commonsCli"])

    infomodelBundle("javax.validation", "validation-api", libraryVersions["javaxValidation"])

    infomodelBundle("com.fasterxml.jackson.core", "jackson-annotations", libraryVersions["jackson"])
    infomodelBundle("com.fasterxml.jackson.core", "jackson-databind", libraryVersions["jackson"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])
}
