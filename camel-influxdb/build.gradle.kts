@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

dependencies {
    val influxFeature by configurations

    influxFeature("org.influxdb", "influxdb-java", libraryVersions["influxDB"])
    influxFeature("org.apache.camel", "camel-influxdb", libraryVersions["camel"])
}
