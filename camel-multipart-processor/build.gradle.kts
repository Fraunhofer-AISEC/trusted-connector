dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.extra.get("libraryVersions") as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    providedByFeature("org.apache.camel", "camel-core", libraryVersions["camel"])
	providedByFeature("org.apache.camel", "camel-http4", libraryVersions["camelHttp4"])

    providedByBundle("org.apache.httpcomponents", "httpcore-osgi", libraryVersions["httpcore"])
	providedByBundle("org.apache.httpcomponents", "httpclient-osgi", libraryVersions["httpclient"])
    
    providedByBundle("commons-fileupload", "commons-fileupload", libraryVersions["commonsFileUpload"])

    osgiCore("org.apache.felix", "org.apache.felix.framework", libraryVersions["felixFramework"])
    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    testImplementation("junit", "junit", libraryVersions["junit4"])
    testImplementation("org.mockito", "mockito-core", libraryVersions["mockito"])
    testImplementation("org.apache.camel", "camel-test", libraryVersions["camel"])
}
