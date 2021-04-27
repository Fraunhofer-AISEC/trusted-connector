@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    providedByBundle(project(":ids-api")) { isTransitive = false }

    osgiCore("org.osgi", "org.osgi.core", libraryVersions["osgi"])

    providedByFeature("org.eclipse.jetty", "jetty-util", libraryVersions["jetty"])
}
