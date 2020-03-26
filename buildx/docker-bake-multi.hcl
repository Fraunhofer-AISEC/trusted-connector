# Caching not practical here, since build from base image is trivial
target "jdk-base" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "build-container" {
  output = ["type=registry"]
  cache-to = ["fraunhoferaisec/build-container:cache"]
  cache-from = ["fraunhoferaisec/build-container:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "core" {
  output = ["type=registry"]
  cache-to = ["fraunhoferaisec/trusted-connector-core:cache"]
  cache-from = ["fraunhoferaisec/trusted-connector-core:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "tpmsim" {
  output = ["type=registry"]
  cache-to = ["fraunhoferaisec/tpmsim:cache"]
  cache-from = ["fraunhoferaisec/tpmsim:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

# Caching not practical here, since build from jdk-base is trivial
target "ttpsim" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "example-idscp-consumer-app" {
  output = ["type=registry"]
  cache-to = ["fraunhoferaisec/example-server:cache"]
  cache-from = ["fraunhoferaisec/example-server:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "example-idscp-provider-app" {
  output = ["type=registry"]
  cache-to = ["fraunhoferaisec/example-client:cache"]
  cache-from = ["fraunhoferaisec/example-client:cache"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}
