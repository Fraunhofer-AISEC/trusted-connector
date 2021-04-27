import com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesExtension
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleDescriptor
import com.github.lburgazzoli.gradle.plugin.karaf.features.model.FeatureDescriptor

@Suppress("UNCHECKED_CAST")
val libraryVersions = rootProject.extra.get("libraryVersions") as Map<String, String>

plugins {
    id("com.github.lburgazzoli.karaf") version "0.5.1"
}

description = "IDS Karaf Feature"

val bundleDependencies = ArrayList<Pair<String, String>>()
val bundleConfigurations = ArrayList<String>()

configurations.filter { it.name.endsWith("Bundle") }.forEach { bundleConfigurations.add(it.name) }

rootProject.subprojects
    .filter { it.name.matches(Regex("(^camel-.*|ids-.*|.*-(patch|wrapper)\$)")) }
    .forEach { p ->
        var included = false
        // Include each sub-project declaring dependencies with a configuration ending on "Bundle",
        // using exactly that configuration for the dependency itself and selection of additional recursive dependencies.
        p.configurations.filter { it.name.endsWith("Bundle") || it.name.endsWith("Feature") }.forEach { c ->
            // Resolve the configuration to obtain dependencies
            c.resolve()
            if (c.dependencies.size > 0) {
                included = true
                bundleDependencies += c.name to p.name
            }
        }
        // If project has not been included, include it using "providedByBundle"
        if (!included) {
            bundleDependencies += "providedByBundle" to p.name
        }
    }

dependencies {
    // Include the dependencies calculated above
    bundleDependencies.forEach {
        add(it.first, project(path = ":${it.second}", configuration = it.first))
    }
    zmqFeature("org.apache-extras.camel-extra", "camel-zeromq", libraryVersions["camelZeroMQ"])
    providedByBundle("org.jetbrains.kotlin", "kotlin-osgi-bundle", libraryVersions["kotlin"])
    providedByBundle("jakarta.activation:jakarta.activation-api:1.2.2")
}

karaf {
    features(closureOf<KarafFeaturesExtension> {
        xsdVersion = "1.3.0"
        // it seems to be best practice to include the version in the feature repository as well
        name = "ids-${project.version}"
        version = project.version as String

        // the camel repository
        repository("mvn:org.apache.camel.karaf/apache-camel/${libraryVersions["camel"]}/xml/features")

        // include cxf repository, since the camel one is slightly outdated
//        repository "mvn:org.apache.cxf.karaf/apache-cxf/${libraryVersions.cxf}/xml/features"
        
        feature(closureOf<FeatureDescriptor> {
            name = "ids"
            description = "IDS Feature"

            // only specifiy the dependencies that will be provided by bundles, the rest will be specified via features
            configurations(*bundleConfigurations.toTypedArray())

            // standard feature, but we need to explicitly state it here so that it gets installed in our bare Karaf
            feature("jetty")

            // CXF for REST APIs
            feature("cxf-jaxrs")
            feature("cxf-jackson")

            // CXF commands for the shell
            /*
             Notice: This should actually be a conditional dependency, only if shell is available.
             however the karaf assemble plugin does not seem to resolve these conditions during packaging.
            */
            feature("cxf-commands")

		    // For Web Application Bundles, such as our WebConsole
            feature("war")

            // Features for Apache Camel routing
            feature("camel-jackson")
            feature("camel-milo")
            feature("camel-cxf")
            feature("camel-ahc")
            feature("camel-http")
            feature("camel-jetty")
            feature("camel-paho")
            //feature("camel-mqtt")
            feature("camel-jsonpath")
            feature("camel-csv")

            // start our bundles a lit bit later
            bundle ("de.fhg.aisec.ids", closureOf<BundleDescriptor> {
                attribute("start-level", "90")
            })
        })

        feature(closureOf<FeatureDescriptor> {
            name = "camel-influxdb-aisec"
            description = "Correctly packed version of camel-influxdb feature"
            version = libraryVersions["camel"]

            //noinspection GroovyAssignabilityCheck
            configurations("influxFeature")
        })

        feature(closureOf<FeatureDescriptor> {
            name = "camel-zeromq-aisec"
            description = "Camel ZeroMQ feature"
            version = libraryVersions["camelZeroMQ"]

            //noinspection GroovyAssignabilityCheck
            configurations("zmqFeature")
        })
    })
}
