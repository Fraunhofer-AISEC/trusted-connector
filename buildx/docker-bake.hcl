target "build-container" {
  output = ["type=docker"]
}

target "core" {
  output = ["type=docker"]
}

//target "tpmsim" {
//  output = ["type=docker"]
//}