//
//  NendoroidSet.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

public struct NendoroidSet: Base {
    
    public func location() -> String {
        return "Set/\(String(format: "Set%03d", num.toInt()!)).json"
    }

    public let num: String
    public var setName: String
    public var list: [String]
    
    public init(num: String, setName: String, list: [String]) {
        self.num = num
        self.setName = setName
        self.list = list
    }
}
