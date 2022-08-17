target "build-container" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8", "linux/arm/v7"]
}

target "core" {
  output = ["type=registry"]
  platforms = ["linux/amd64", "linux/arm64/v8"]
}