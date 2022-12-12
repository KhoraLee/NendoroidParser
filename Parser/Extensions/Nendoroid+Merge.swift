//
//  Nendoroid+Merge.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/13.
//

import Foundation

extension Nendoroid {
    mutating func merge(with new: Nendoroid) throws {
        if self.num != new.num { return }
        try self.name.join(new.name)
        try self.series.join(new.series)
        if self.price == -1 { self.price = new.price }
        self.releaseDate.append(contentsOf: new.releaseDate)
        if self.gender == nil { self.gender = new.gender }
    }
}
