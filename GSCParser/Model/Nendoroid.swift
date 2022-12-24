//
//  Nendoroid.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

// MARK: - Nendoroid

public struct Nendoroid: Base, Hashable {

  // MARK: Lifecycle

  public init(
    num: String,
    name: LocalizedString = LocalizedString(),
    series: LocalizedString = LocalizedString(),
    gscProductNum: Int = -1,
    price: Int = -1,
    releaseDate: [String] = [],
    image: String = "",
    gender: Gender? = nil,
    set: Int? = nil)
  {
    self.num = num
    self.name = name
    self.series = series
    self.gscProductNum = gscProductNum
    self.price = price
    self.releaseDate = releaseDate
    self.image = image
    self.gender = gender
    self.set = set
  }

  // MARK: Public

  public let num: String
  public var image: String
  public var releaseDate: [String]
  public var name, series: LocalizedString
  public var gscProductNum, price: Int
  public var set: Int?
  public var gender: Gender?

  public static func == (lhs: Nendoroid, rhs: Nendoroid) -> Bool {
    lhs.num == rhs.num && lhs.gscProductNum == rhs.gscProductNum
  }

  public func hash(into hasher: inout Hasher) {
    hasher.combine(num)
    hasher.combine(gscProductNum)
  }

  public func location() -> String {
    let range = num.components(separatedBy: .decimalDigits.inverted).joined().toInt()! / 100
    let folderName = String(format: "%04d-%04d", range * 100, (range + 1) * 100 - 1)
    return "\(folderName)/\(num).json"
  }

  // MARK: Internal

  enum CodingKeys: String, CodingKey {
    case num, name, series, price, image, gender, set
    case gscProductNum = "gsc_productNum"
    case releaseDate = "release_date"
  }
}

extension Nendoroid {
  mutating func merge(with new: Nendoroid) throws {
    if num != new.num { return }
    try name.join(new.name)
    try series.join(new.series)
    if price == -1 { price = new.price }
    if releaseDate.isEmpty { releaseDate = new.releaseDate }
    if gender == nil { gender = new.gender }
  }
}
