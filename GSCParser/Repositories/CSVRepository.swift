//
//  CSVRepository.swift
//  GSCParser
//
//  Created by 이승윤 on 2022/12/25.
//

import Foundation
import SwiftCSV

open class CSVRepository {

  // MARK: Lifecycle

  public init(csvPath: URL) throws {
    csv = try CSV<Enumerated>(url: csvPath, delimiter: .comma, loadColumns: false)
  }

  // MARK: Public

  public func getNendoroidList() -> [Nendoroid] {
    var list = [Nendoroid]()
    for record in csv.rows where !record[1].isEmpty {
      list.append(Nendoroid(
        num: record[0],
        name: LocalizedString(locale: .ko, string: record[1]),
        series: LocalizedString(locale: .ko, string: record[2]),
        gender: genderDict[record[3]]))
    }
    return list
  }

  public func getNendoroidSetList() -> [NendoroidSet] {
    var list = [NendoroidSet]()
    var serial = 1
    for record in csv.rows where !(record[1].isEmpty || record[4].isEmpty) {
      if let existing = list.firstIndex(where: { $0.setName == record[4] }) {
        list[existing].list.append(record[0])
      } else {
        list.append(NendoroidSet(num: "\(serial)", setName: record[4], list: [record[0]]))
        serial += 1
      }
    }
    return list
  }

  // MARK: Private

  private var csv: CSV<Enumerated>

  private let genderDict = [
    "남": Gender.male,
    "여": Gender.female,
    "남, 여": Gender.maleAndFemale,
    "양성": Gender.androgyny,
    "다요": Gender.dayo,
    "?": Gender.unknown,
  ]

}
