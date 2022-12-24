// swift-tools-version:5.6
import PackageDescription

let package = Package(
  name: "BuildTools",
  platforms: [.macOS(.v10_11)],
  dependencies: [
    .package(url: "https://github.com/nicklockwood/SwiftFormat", from: "0.50.6"),
    .package(url: "https://github.com/realm/SwiftLint", revision: "a876e860ee0e166a05428f430888de5d798c0f8d"),
  ],
  targets: [.target(name: "BuildTools", path: "")])
