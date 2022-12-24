//
//  ParserHelper.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/10.
//

import Foundation

class ParserHelper {
  static func parseReleaseDate(dateString: String) -> [String] {
    let list = dateString
      .replacingOccurrences(of: "年月", with: "/") // Because GSC product page have typo
      .replacingOccurrences(of: "年", with: "/")
      .replacingOccurrences(of: "月", with: "-")
      .replacing(try! Regex("\\d次"), with: "")
      .replacing(try! Regex("[^\\d/-]"), with: "")
      .split(separator: "-")
      .map { String($0) }
    return list.compactMap { element in
      var date: String {
        if element.starts(with: "/") {
          return element.substring(1)
        } else {
          return element
        }
      }
      switch date.count {
      case 6:
        return "\(date.substring(from: 0, to: date.count - 2))0\(date.substring(date.count - 1))"
      case 7:
        return date
      default:
        return nil
      }
    }
  }
}
