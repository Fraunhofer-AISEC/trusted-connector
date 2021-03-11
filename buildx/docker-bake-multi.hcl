# Caching not practical here, since build from base image is trivial
target "jdk-base" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "build-container" {
  output = ["type=registry"]
//  cache-to = ["registry.netsec.aisec.fraunhofer.de/tc-build-container:cache"]
//  cache-from = ["registry.netsec.aisec.fraunhofer.de/tc-build-container:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "core" {
  output = ["type=registry"]
//  cache-to = ["registry.netsec.aisec.fraunhofer.de/tc-core:cache"]
//  cache-from = ["registry.netsec.aisec.fraunhofer.de/tc-core:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

//target "tpmsim" {
//  output = ["type=registry"]
////  cache-to = ["registry.netsec.aisec.fraunhofer.de/tc-tpmsim:cache"]
////  cache-from = ["registry.netsec.aisec.fraunhofer.de/tc-tpmsim:cache"]
//  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
//}