rootProject.name = "trusted-connector-core"

include(":camel-influxdb")
include(":camel-multipart-processor")
// include(":ids-acme")
include(":ids-api")
include(":ids-container-manager")
include(":ids-dataflow-control")
// include(":ids-dynamic-tls")
include(":ids-endpoint-config")
include(":ids-infomodel-manager")
include(":ids-route-manager")
include(":ids-settings")
include(":ids-webconsole")

include(":ids-connector")

// will be extracted to a separate repository later
include(":example-idscp2-client-server")
