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

  // MARK: Public

  public enum ListParseOption {
    case announce, range, release
  }

  public static let shared = GSCRepository()

  public func getNendoroidInfo(nendoroid: Nendoroid) async -> Nendoroid {
    var result = nendoroid
    let nendoroidJA = await getNendoroidInfo(locale: .ja, number: nendoroid.num, productID: nendoroid.gscProductNum)
    let nendoroidEN = await getNendoroidInfo(locale: .en, number: nendoroid.num, productID: nendoroid.gscProductNum)
    if nendoroidJA != nil { try? result.merge(with: nendoroidJA!) }
    if nendoroidEN != nil { try? result.merge(with: nendoroidEN!) }
    return result
  }

  public func parseNendoroidList(option: [ListParseOption] = [.announce, .range, .release]) async -> Set<Nendoroid> {
    await withTaskGroup(of: Set<Nendoroid>.self) { group in
      var list = Set<Nendoroid>()
      if option.contains(.announce) {
        group.addTask { await self.getNendoroidListbyYear(locale: .ja, by: .announce) }
        group.addTask { await self.getNendoroidListbyYear(locale: .en, by: .announce) }
      }
      if option.contains(.release) {
        group.addTask { await self.getNendoroidListbyYear(locale: .ja, by: .release) }
        group.addTask { await self.getNendoroidListbyYear(locale: .en, by: .release) }
      }
      if option.contains(.range) {
        group.addTask { await self.getNendoroidListbyRange(locale: .ja) }
        group.addTask { await self.getNendoroidListbyRange(locale: .en) }
      }
      // 1522-DX is missing on all three above. So need to added manually
      list.insert(Nendoroid(
        num: "1522-DX",
        gscProductNum: 10257,
        image: "https://images.goodsmile.info/cgm/images/product/20201020/10257/77495/thumb/878c6adf472a36a232fdb8085534dd4b.jpg"))
      for await sets in group {
        list.formUnion(sets)
      }
      // TODO: There is some nendoroids that have different gsc product num for each locale. So need to deal with them. (ex, 1982 -> ja: 13448, en: 13449)
      return list
    }
  }

  // MARK: Private

  private let convertionDict = [
    "商品名" : "name",
    "Product Name" : "name",
    "作品名" : "series",
    "Series" : "series",
    "価格" : "price",
    "Price" : "price",
    "再販" : "release_info",
    "発売時期" : "release",
    "Release Date" : "release",
  ]

  private func getNendoroidListbyRange(locale: LanguageCode) async -> Set<Nendoroid> {
    await withTaskGroup(of: Set<Nendoroid>.self) { group in
      var nendoroids = Set<Nendoroid>()
      for i in 1...21 {
        group.addTask {
          var list = Set<Nendoroid>()
          do {
            let range = String(format: "%03d-%d", (i - 1) * 100 + (i == 1 ? 0 : 1), i * 100)
            let request = AF.request(GSCRouter.byRange(locale: locale, range: range)).serializingString()
            let doc = try SwiftSoup.parse(await request.value)
            let elements = try doc.select("div.hitItem").select("div > a")
            for e in elements {
              guard let nendoroid = try? self.parseBaesNendoroid(locale: locale, element: e) else { continue }
              list.insert(nendoroid)
            }
          } catch {
            print(error.localizedDescription)
          }
          return list
        }
      }
      for await list in group {
        nendoroids.formUnion(list)
      }
      return nendoroids
    }
  }

  private func getNendoroidListbyYear(locale: LanguageCode, by type: GSCRouter.SortType) async -> Set<Nendoroid> {
    await withTaskGroup(of: Set<Nendoroid>.self) { group in
      var nendoroids = Set<Nendoroid>()
      for year in 2005...2024 {
        group.addTask {
          var list = Set<Nendoroid>()
          do {
            let request = AF.request(GSCRouter.byYear(locale: locale, type: type, year: year)).serializingString()
            let doc = try SwiftSoup.parse(await request.value)
            let elements = try doc
              .select("[class=\"hitItem nendoroid nendoroid_series\"], [class=\"hitItem nendoroid_series\"]")
              .select("div > a")
            for e in elements {
              guard let nendoroid = try? self.parseBaesNendoroid(locale: locale, element: e) else { continue }
              list.insert(nendoroid)
            }
          } catch {
            print(error.localizedDescription)
          }
          return list
        }
      }
      for await list in group {
        nendoroids.formUnion(list)
      }
      return nendoroids
    }
  }

  private func parseBaesNendoroid(locale: LanguageCode, element: Element) throws -> Nendoroid? {
    let num = try element.select("span.hitNum").text().convertToHalfWidthString().lowercased()
      .replacingOccurrences(of: "‐", with: "-")
      .replacingOccurrences(of: "-", with: "")
      .replacingOccurrences(of: "dx", with: "-DX")
    if num == "" { return nil }
    guard
      let gscCode = try String(
        element.attr("href")
          .replacingOccurrences(of: "https://www.goodsmile.info/\(locale)/product", with: "").split(separator: "/").first!)
        .toInt() else { return nil }
    let imageLink = try "https:" + element.select("img").attr("data-original")
    return Nendoroid(num: num, gscProductNum: gscCode, image: imageLink)
  }

  private func getNendoroidInfo(locale: LanguageCode, number: String, productID: Int) async -> Nendoroid? {
    do {
      let html = try await getProductInfo(locale: locale, productID: productID)
      let document = try SwiftSoup.parse(html)
      let elements = try document.select("div.itemDetail").select("div.detailBox>dl")
      let keyElements = try elements.select("dt").map { try $0.text() }
      let valueElements = try elements.select("dd").map { try $0.text() }
      if keyElements.count == 0 { return nil }
      if keyElements.count != valueElements.count { throw GSCError.keyValueSizeMismatch }

      var info = [String: String]()
      for i in 0...(keyElements.count - 1) {
        let key = keyElements[i]
        if !convertionDict.keys.contains(key) || info.keys.contains(convertionDict[key]!) { continue }
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
        price: info["price"]?.replacing(try Regex("\\D"), with: "").toInt() ?? 0,
        releaseDate: Array(releaseDates).sorted())
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
