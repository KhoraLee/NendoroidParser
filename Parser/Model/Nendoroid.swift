//
//  Nendoroid.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/02.
//

public struct Nendoroid: Base {
    public func location() -> String {
        let range = num.components(separatedBy: .decimalDigits.inverted).joined().toInt()! / 100
        let folderName = String(format: "%04d-%04d", range * 100, (range + 1) * 100 - 1)
        return "\(folderName)/\(num).json"
    }
    
    public let num: String
    public var image: String
    public var releaseDate: [String]
    public var name, series : LocalizedString
    public var gscProductNum, price : Int
    public var set: Int?
    public var gender: Gender?
    
    enum CodingKeys: String, CodingKey {
        case num, name, series, price, image, gender, set
        case gscProductNum = "gsc_productNum"
        case releaseDate = "release_date"
    }
    
    public init(num: String, name: LocalizedString, series: LocalizedString, gscProductNum: Int, price: Int, releaseDate: [String], image: String = "", gender: Gender? = nil, set: Int? = nil) {
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
}
