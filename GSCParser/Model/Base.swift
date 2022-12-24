//
//  Base.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

import Foundation

// MARK: - Base

public protocol Base: Codable {
  func location() -> String
}

extension Base {
  public func save() throws {
    try NendoroidDAO.shared.saveFile(data: self, to: location())
  }
}
