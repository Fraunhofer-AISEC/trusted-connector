target "build-container" {
  output = ["type=docker"]
  context = "../build-container"
  tags = ["fraunhoferaisec/docker-build:develop"]
}

target "core" {
  output = ["type=docker"]
  context = "../ids-connector"
  tags = ["fraunhoferaisec/trusted-connector-core:develop"]
}
