//
//  String+Additions.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

import Foundation

extension String {
    func toInt() -> Int? {
        Int(self)
    }
    
    public var containsFullWidthCharacters: Bool {
        unicodeScalars.contains { $0.isFullWidth }
    }
    
    public func convertToHalfWidth() throws -> String {
        if (!self.containsFullWidthCharacters || !unicodeScalars.contains { $0.isConverterable }) {
            throw UnicodeScalar.EAWError.noConvertableFullWidthCharacter
        }
        return String(unicodeScalars.map {
            if !$0.isConverterable { return $0 }
            if $0.value == 0x3000 {
                return UnicodeScalar(0x0020)
            } else {
                return UnicodeScalar($0.value - 0xfee0)!
            }
        }.map(Character.init))
        
    }
}
