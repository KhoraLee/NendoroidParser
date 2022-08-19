package utils

import data.Nendoroid
import extensions.fullToHalf
import extensions.isFullWidth
import extensions.load
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object GSCParser {
    private val keyMap = mapOf(
        "商品名" to "name",
        "Product Name" to "name",
        "作品名" to "series",
        "Series" to "series",
        "価格" to "price",
        "Price" to "price",
        "再販" to "release_info",
        "発売時期" to "release",
        "Release Date" to "release"
    )

    fun parseByYear(locale: Locale = Locale.JAPANESE) {
        val year = (2006..2022).toList()
        val baseURL = "https://www.goodsmile.info/$locale/products/category/nendoroid_series/announced/"

        year.forEach {
            val document = Jsoup.connect(baseURL + it.toString()).get()
            val elements = document
                .select("[class=\"hitItem nendoroid nendoroid_series\"], [class=\"hitItem nendoroid_series\"]")
                .select("div.hitBox>a")
            elements.forEach { ele ->
                updateNendoroid(ele, locale)
            }
        }
    }

    fun parseByNumber(locale: Locale = Locale.JAPANESE) {
        val numSet = mutableListOf<String>()
        for (i in 0..19) {
            numSet.add(String.format("%03d-%d", if (i != 0) i * 100 + 1 else 0, (i + 1) * 100))
        }
        println("fun parseGoodSmileNumber() - Current Locale is $locale")
        val baseURL = "https://www.goodsmile.info/$locale/nendoroid"

        numSet.forEach {
            val document = try {
                Jsoup.connect(baseURL + it).get()
            } catch (e: Exception) {
                return@forEach
            }
            val elements = document
                .select("div.hitItem")
                .select("div.hitBox>a")
            elements.forEach { ele ->
                updateNendoroid(ele, locale)
            }
        }
    }

    fun updateNendoroids() {
        val nendoroidList = getAllNendoroids()
        val total = nendoroidList.size
        val current = AtomicInteger()
        nendoroidList.parallelStream().forEach {
            current.incrementAndGet()
            if (it.nameWithoutExtension == "1538") return@forEach // 특설페이지로 파싱 불가
            val diskNendoroid: Nendoroid = Nendoroid(it.nameWithoutExtension).load() ?: return@forEach
            val parsedNendoroid = getNendoroid(diskNendoroid)
            diskNendoroid.merge(parsedNendoroid)
            diskNendoroid.save()
            val logMsg = String.format(
                "[%02.2f%%] %-8s Thread : (%s)",
                (current.get().toFloat() * 100 / total),
                diskNendoroid.num,
                Thread.currentThread().name
            )
            println(logMsg)
            Thread.sleep(20)
        }
    }

    private fun getAllNendoroids(path: String = ""): ArrayList<File> {
        val nendoroidList = ArrayList<File>()
        val file = File(path.ifEmpty { "../NendoroidDB/Nendoroid" })
        val fList = file.listFiles()
        fList.forEach {
            if (it.isDirectory && it.name != "Set") {
                nendoroidList.addAll(getAllNendoroids(it.path))
                return@forEach
            }
            nendoroidList.add(it)
        }
        return nendoroidList
    }

    private fun updateNendoroid(element: Element, locale: Locale) {
        val nendoroid = getNendoroid(element, locale)
        val nendoroidFromDisk: Nendoroid? = Nendoroid(nendoroid.num).load()
        if (nendoroidFromDisk != null) {
            nendoroid.merge(nendoroidFromDisk)
        }
        nendoroid.save()
    }

    fun getNendoroid(nendoroid: Nendoroid): Nendoroid {
        val nendoroidJA = getNendoroid(nendoroid.num, nendoroid.gsc_productNum, Locale.JAPANESE)
        val nendoroidEN = getNendoroid(nendoroid.num, nendoroid.gsc_productNum, Locale.ENGLISH)
        if(nendoroidJA == null && nendoroidEN != null) {
            return nendoroidEN
        } else if(nendoroidJA != null && nendoroidEN == null) {
            return nendoroidJA
        }
        nendoroidJA!!.merge(nendoroidEN!!)
        return nendoroidJA
    }

    private fun getNendoroid(number: String, gsc: Int, locale: Locale): Nendoroid? {
        val url = "https://www.goodsmile.info/$locale/product/$gsc"

        val document = try {
            Jsoup.connect(url).get()
        } catch (e: Exception) {
            System.err.println("[Error] : $number $gsc doesn't exist")
            return null
        }
        val elements = document.select("div.itemDetail").select("div.detailBox>dl")
        val keyElement = elements.select("dt")
        val valueElement = elements.select("dd")

        val kvMap = mutableMapOf<String, String>()
        for (i in 0 until keyElement.size) {
            val key = keyElement[i].text()
            if (keyMap.containsKey(key) && !kvMap.containsKey(keyMap[key])) kvMap[keyMap[key]!!] = valueElement[i].text()
        }

        val release = mutableSetOf<String>()
        if (locale == Locale.JAPANESE) { // 【販売：2017年9月】【再販：2019年5月】【2次再販：2022年10月】
            if (kvMap.containsKey("release_info")) {
                when (number) {
                    "042" -> {
                        release.add("2008/08")
                        release.add("2012/07")
                        release.add("2013/01")
                    }
                    "149" -> {
                        release.add("2011/02")
                        release.add("2011/06")
                        release.add("2012/12")
                    }
                    else -> {
                        val dateList = parseReleaseDate(kvMap["release_info"]!!)
                        dateList.forEach {
                            release.add(it)
                        }
                    }
                }
            } else {
                when (number) {
                    "439" -> release.add("2014/12")
                    "587" -> release.add("2016/04")
                    "626" -> {
                        release.add("2016/08")
                        release.add("2019/01")
                    }
                    "652" -> {
                        release.add("2016/12")
                        kvMap["price"] = "4500"
                    }
                    "1325" -> release.add("2020/12")
                    else -> {
                        val dateList = parseReleaseDate(kvMap["release"]!!)
                        dateList.forEach {
                            release.add(it)
                        }
                    }
                }
            }
            when (number) {
                "267" -> kvMap["price"] = "3909"
                "378b" -> kvMap["price"] = "5500"
                "819" -> kvMap["price"] = "4800"
                "1291" -> kvMap["price"] = "5500"
                "1672b" -> kvMap["price"] = "5900"
            }
        }
        return Nendoroid(
            num = number,
            gsc_productNum = gsc,
            name = mutableMapOf(locale to kvMap["name"]!!),
            series = mutableMapOf(locale to kvMap["series"]!!),
            price = kvMap["price"]?.replace("\\D".toRegex(), "")?.toIntOrNull() ?: -1,
            release_date = release
        )
    }

    private fun getNendoroid(element: Element, locale: Locale = Locale.JAPANESE): Nendoroid {
        val gscProductNumber = element.attr("href")
            .replace("https://www.goodsmile.info/$locale/product/", "")
            .split("/")[0]
            .toInt()
        val image = "https:" + element.select("img").attr("data-original")
        //var name = element.select("span.hitTtl").text()
        var num = element.select("span.hitNum").text()
            .lowercase()
            .replace("‐", "-") // 같아 보이나 서로 다른 특문; 가끔 생김.. 원인 불명
            .replace("-", "")
            .replace("dx", "-DX")

        if (num.lastOrNull()?.isFullWidth() == true) { //전각 a,b 반각으로 변환
            val c = num.last().fullToHalf()
            num = num.substring(0, num.lastIndex) + c
            println("Changed num $num")
        }

        return Nendoroid(num = num, image = image, gsc_productNum = gscProductNumber)
    }

    private fun parseReleaseDate(dateString: String): List<String> {
        val list = dateString.replace("年月", "/") // 굿스마 공홈 오타 대응 (ex 214)
            .replace("年", "/")
            .replace("月", "-")
            .replace("\\d次".toRegex(), "")
            .replace("[^\\d/-]".toRegex(), "")
            .split("-")
        val result = mutableListOf<String>()
        list.forEach {
            val date = if (it.startsWith("/")) {
                it.substring(1)
            } else it
            when (date.length) {
                6 -> {
                    var str = date.substring(0, date.length - 1)
                    str += "0" + date.substring(date.length - 1)
                    result.add(str)
                }
                7 -> result.add(date)
                else -> return@forEach
            }
        }
        return result
    }

}