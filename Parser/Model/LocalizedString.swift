//
//  LocalizedString.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

public struct LocalizedString: Codable {
    
    var en: String?
    var ja: String?
    var ko: String?
    
    public init() { }
    
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
