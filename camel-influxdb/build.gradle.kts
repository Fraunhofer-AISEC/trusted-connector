@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    implementation("org.influxdb", "influxdb-java", libraryVersions["influxDB"])
    implementation("org.apache.camel", "camel-influxdb", libraryVersions["camel"])
}
