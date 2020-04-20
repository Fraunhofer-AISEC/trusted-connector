import org.yaml.snakeyaml.Yaml

dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            Yaml().load(File("${rootDir}/libraryVersions.yaml").inputStream()) as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    providedByBundle("org.apache.logging.log4j", "log4j-core", libraryVersions["log4j"])
    providedByBundle("org.apache.logging.log4j", "log4j-slf4j-impl", libraryVersions["log4j"])

    providedByFeature("org.eclipse.jetty", "jetty-util", libraryVersions["jetty"])
    providedByFeature("org.apache.karaf.scheduler", "org.apache.karaf.scheduler.core", libraryVersions["karaf"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    implementation("org.shredzone.acme4j", "acme4j-client", libraryVersions["acme"])
    implementation("org.shredzone.acme4j", "acme4j-utils", libraryVersions["acme"])
    implementation("org.nanohttpd", "nanohttpd", libraryVersions["nanohttpd"])
}
