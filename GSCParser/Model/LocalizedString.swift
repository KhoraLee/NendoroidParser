//
//  LocalizedString.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

public struct LocalizedString: Codable {

  // MARK: Lifecycle

  public init() { }

  public init(locale: LanguageCode, string: String) {
    switch locale {
    case .ko:
      ko = string
    case .ja:
      ja = string
    case .en:
      en = string
    }
  }

  // MARK: Internal

  var en: String?
  var ja: String?
  var ko: String?

  mutating func join(_ new: LocalizedString) throws {
    if (new.en != nil && en != nil) || (new.ja != nil && ja != nil) || (new.ko != nil && ko != nil) {
      throw GSCError.alreadyHaveValue
    }
    if new.en != nil {
      en = new.en
    }
    if new.ja != nil {
      ja = new.ja
    }
    if new.ko != nil {
      ko = new.ko
    }
  }

  func localizedString(locale: LanguageCode) -> String? {
    switch locale {
    case .en:
      return en
    case .ja:
      return ja
    case .ko:
      return ko
    }
  }
}
