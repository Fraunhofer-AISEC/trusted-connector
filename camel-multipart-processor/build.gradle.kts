import org.yaml.snakeyaml.Yaml

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            Yaml().load(File("${rootDir}/libraryVersions.yaml").inputStream()) as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    implementation("de.fraunhofer.iais.eis.ids.infomodel", "java", libraryVersions["infomodel"])
    implementation("de.fraunhofer.iais.eis.ids", "infomodel-serializer", libraryVersions["infomodelSerializer"])
    
    // Bill of Materials (BOM) for Camel
    bom("org.apache.camel", "camel-parent", libraryVersions["camel"])

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])
	providedByFeature("org.apache.camel", "camel-http4", libraryVersions["camelHttp4"])

    compileOnly("org.checkerframework", "checker-qual", libraryVersions["checkerQual"])

    providedByBundle("org.apache.httpcomponents", "httpcore-osgi", libraryVersions["httpcore"])
	providedByBundle("org.apache.httpcomponents", "httpclient-osgi", libraryVersions["httpclient"])
    
    providedByBundle("commons-fileupload", "commons-fileupload", libraryVersions["commonsFileUpload"])

    osgiCore("org.apache.felix", "org.apache.felix.framework", libraryVersions["felixFramework"])
    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
