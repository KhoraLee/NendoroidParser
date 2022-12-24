//
//  NendoroidDAO.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

import Foundation

open class NendoroidDAO {

  // MARK: Lifecycle

  private init() { } // Block creating new instance

  // MARK: Open

  open func loadNendoroid(number num: String) throws -> Nendoroid {
    let range = num.components(separatedBy: .decimalDigits.inverted).joined().toInt()! / 100
    let folderName = String(format: "%04d-%04d", range * 100, (range + 1) * 100 - 1)
    let url = path.appending(components: folderName, num).appendingPathExtension("json")
    return try loadFile(at: url)
  }

  open func loadNendoroidSet(number num: Int) throws -> NendoroidSet {
    let url = path.appending(components: "Set", String(format: "Set%03d", num)).appendingPathExtension("json")
    return try loadFile(at: url)
  }

  open func getAllNendoroidNums() throws -> [String] {
    var nendoList: [String] = []
    let folders = try fm.contentsOfDirectory(at: path, includingPropertiesForKeys: nil)
      .filter({ $0.hasDirectoryPath })
      .filter({ $0.lastPathComponent != "Set" })
      .sorted(by: { $0.lastPathComponent < $1.lastPathComponent })
    try folders.forEach { folder in
      let nendoroids = try fm.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil)
        .filter({ $0.pathExtension == "json" })
        .map({ $0.deletingPathExtension() })
        .map({ $0.lastPathComponent })
        .sorted()
      nendoList.append(contentsOf: nendoroids)
    }
    return nendoList
  }

  // TODO: Implement after creating csv reader
//    func loadAllNendoroidSet() throws -> [NendoroidSet] {
//
//    }

  // MARK: Public

  public static let shared = NendoroidDAO()

  public static func setup(path: URL) {
    if ppath != nil {
      fatalError("NendoroidDAO is already setuped.")
    }
    ppath = path
  }

  // MARK: Internal

  let fm = FileManager.default

  var path: URL {
    get {
      if NendoroidDAO.ppath == nil {
        fatalError("NendoroidDAO is used before setup")
      }
      return NendoroidDAO.ppath!
    }
  }

  func saveFile<T>(data: T, to path: String) throws where T: Encodable {
    let url = self.path.appending(path: path)
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .withoutEscapingSlashes, .sortedKeys]
    try encoder.encode(data).write(to: url)
  }

  // MARK: Private

  private static var ppath: URL?

  private func loadFile<T>(at url: URL) throws -> T where T: Decodable {
    try JSONDecoder().decode(T.self, from: Data(contentsOf: url))
  }
}
