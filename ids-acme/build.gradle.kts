dependencies {
    @Suppress("UNCHECKED_CAST") val libraryVersions =
            rootProject.extra.get("libraryVersions") as Map<String, String>

    providedByBundle(project(":ids-api")) { isTransitive = false }

    providedByFeature("org.eclipse.jetty", "jetty-util", libraryVersions["jetty"])
    providedByFeature("org.apache.karaf.scheduler", "org.apache.karaf.scheduler.core", libraryVersions["karaf"])

    osgiCore("org.osgi", "osgi.cmpn", libraryVersions["osgiCompendium"])

    implementation("org.shredzone.acme4j", "acme4j-client", libraryVersions["acme"])
    implementation("org.shredzone.acme4j", "acme4j-utils", libraryVersions["acme"])
    implementation("org.nanohttpd", "nanohttpd", libraryVersions["nanohttpd"])
}
