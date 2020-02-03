target "build-container" {
  output = ["type=docker"]
}

target "core" {
  output = ["type=docker"]
}

target "tpmsim" {
  output = ["type=docker"]
}

target "ttpsim" {
  output = ["type=docker"]
}

target "example-idscp-consumer-app" {
  output = ["type=docker"]
}

target "example-idscp-provider-app" {
  output = ["type=docker"]
}