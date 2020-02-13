target "jdk-base" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "build-container" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "core" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "tpmsim" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "ttpsim" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "example-idscp-consumer-app" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "example-idscp-provider-app" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}
