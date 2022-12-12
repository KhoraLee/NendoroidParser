//
//  GSCRepository.swift
//  Parser
//
//  Created by 이승윤 on 2022/12/10.
//

import Alamofire
import Foundation
import SwiftSoup

open class GSCRepository {
    public static let shared = GSCRepository()
    
    private let convertionDict = [
        "商品名" : "name",
        "Product Name" : "name",
        "作品名" : "series",
        "Series" : "series",
        "価格" : "price",
        "Price" : "price",
        "再販" : "release_info",
        "発売時期" : "release",
        "Release Date" : "release"
    ]

    
    public func getNendoroidInfo(number: String, productID: Int) async -> Nendoroid {
        var nendoroidJA = await getNendoroidInfo(locale: .ja, number: number, productID: productID)
        let nendoroidEN = await getNendoroidInfo(locale: .en, number: number, productID: productID)
        if nendoroidJA != nil && nendoroidEN == nil {
            return nendoroidJA!
        } else if nendoroidJA == nil && nendoroidEN != nil {
            return nendoroidEN!
        }
        try? nendoroidJA!.merge(with: nendoroidEN!)
        return nendoroidJA!
    }
    
    private func getNendoroidInfo(locale: LanguageCode, number: String, productID: Int) async -> Nendoroid? {
        do {
            let html = try await getProductInfo(locale: locale, productID: productID)
            let document = try SwiftSoup.parse(html)
            let elements = try document.select("div.itemDetail").select("div.detailBox>dl")
            let keyElements = try elements.select("dt").map { try $0.text() }
            let valueElements = try elements.select("dd").map { try $0.text() }
            if keyElements.count != valueElements.count { throw GSCError.keyValueSizeMismatch }
            
            var info = [String: String]()
            for i in 0...(keyElements.count - 1) {
                let key = keyElements[i]
                if info.keys.contains(key) || !convertionDict.keys.contains(key) { continue } // TODO: Need to check if this is needed
                info[convertionDict[key]!] = valueElements[i]
            }
            var releaseDates = Set<String>()
            if locale == .ja {
                // Parse release date and manually insert missing release date
                switch number {
                case "042":
                    releaseDates.insert("2008/08")
                    releaseDates.insert("2012/07")
                    releaseDates.insert("2013/01")
                case "149":
                    releaseDates.insert("2011/02")
                    releaseDates.insert("2011/06")
                    releaseDates.insert("2012/12")
                case "439":
                    releaseDates.insert("2014/12")
                case "587":
                    releaseDates.insert("2016/04")
                case "626":
                    releaseDates.insert("2016/08")
                    releaseDates.insert("2019/01")
                case "652":
                    releaseDates.insert("2016/12")
                case "1325":
                    releaseDates.insert("2020/12")
                default:
                    if info.keys.contains("release_info") {
                        releaseDates = Set(ParserHelper.parseReleaseDate(dateString: info["release_info"]!))
                    } else {
                        releaseDates = Set(ParserHelper.parseReleaseDate(dateString: info["release"]!))
                    }
                }
                // Manually insert missing price
                switch number {
                case "267":
                    info["price"] = "3909"
                case "378b":
                    info["price"] = "5500"
                case "652":
                    info["price"] = "4500"
                case "819":
                    info["price"] = "4800"
                case "1291":
                    info["price"] = "5500"
                case "1672b":
                    info["price"] = "5900"
                default:
                    break
                }
            }
            return Nendoroid(
                num: number,
                name: LocalizedString(locale: locale, string: info["name"]!),
                series: LocalizedString(locale: locale, string: info["series"]!),
                gscProductNum: productID,
                price: info["price"]!.replacing(try Regex("\\D"), with: "").toInt() ?? -1,
                releaseDate: Array(releaseDates)
            )
        } catch {
            print(error.localizedDescription)
            return nil
        }
    }

    private func getProductInfo(locale: LanguageCode, productID: Int) async throws -> String {
        let request = AF.request(GSCRouter.productInfo(locale: locale, productID: productID)).serializingString()
        return try await request.value
    }

}
