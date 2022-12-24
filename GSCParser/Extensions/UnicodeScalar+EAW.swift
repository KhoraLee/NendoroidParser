//
//  UnicodeScalar+EAW.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/09.
//

import Foundation

extension UnicodeScalar {
  enum EAWError: Error {
    case noConvertableFullWidthCharacter
  }

  var isFullWidth: Bool {
    switch value {
    case 0x1100...0x115F: return true
    case 0x2329...0x232A: return true
    case 0x2E80...0x2FFB: return true
    case 0x3000...0x303E: return true
    case 0x3041...0x33FF: return true
    case 0x3400...0x4DB5: return true
    case 0x4E00...0x9FBB: return true
    case 0xA000...0xA4C6: return true
    case 0xAC00...0xD7A3: return true
    case 0xF900...0xFAD9: return true
    case 0xFE10...0xFE19: return true
    case 0xFE30...0xFE6B: return true
    case 0xFF01...0xFF60: return true
    case 0xFFE0...0xFFE6: return true
    case 0x20000...0x2A6D6: return true
    case 0x2A6D7...0x2F7FF: return true
    case 0x2F800...0x2FA1D: return true
    case 0x2FA1E...0x2FFFD: return true
    case 0x30000...0x3FFFD: return true
    default:
      return false
    }
  }

  var isConverterable: Bool {
    switch value {
    case 0x3000: return true
    case 0xFF01...0xFF5E: return true
    default:
      return false
    }
  }
}
