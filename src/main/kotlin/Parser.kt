import data.Gender.*
import data.Nendoroid
import data.NendoroidSet
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.Locale.*
import kotlin.io.path.notExists

class Parser {
    private val basePath = "D:\\NendoroidDB\\Nendoroid"

    val gsc = GSC()
    val csv = CSV()
    private fun copyDirectory(sourceDirectory: File, destinationDirectory: File) {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdir()
        }
        for (f in sourceDirectory.list()) {
            copyDirectoryCompatibilityMode(File(sourceDirectory, f), File(destinationDirectory, f))
        }
    }

    private fun copyDirectoryCompatibilityMode(source: File, destination: File) {
        if (source.isDirectory) {
            copyDirectory(source, destination)
        } else {
            copyFile(source, destination)
        }
    }

    private fun copyFile(sourceFile: File, destinationFile: File) {
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destinationFile).use { out ->
                val buf = ByteArray(1024)
                var length: Int
                while (input.read(buf).also { length = it } > 0) {
                    out.write(buf, 0, length)
                }
            }
        }
    }

    fun init() {
        val ranges = (0..18).toList()
        ranges.forEach {
            val folderName = String.format("%04d-%04d", it * 100, (it + 1) * 100 - 1)
            val path = Paths.get(basePath, folderName)
            if (path.notExists()) Files.createDirectories(path)
        }
        val path = Paths.get(basePath, "Set")
        if (path.notExists()) Files.createDirectories(path)
    }

    fun copyPreJSON() {
        val preDir = File("pre-Nendoroid")
        copyDirectory(preDir, File(basePath))
    }

    // 굿스마 공홈 관련
    class GSC {
        fun parseByYear(locale: Locale = JAPANESE) {
            val year = (2006..2022).toList()
            println("fun parseGoodSmileYear() - Current Locale is $locale")
            val baseURL = "https://www.goodsmile.info/$locale/products/category/nendoroid_series/announced/"

            year.forEach {
                val document = Jsoup.connect(baseURL + it.toString()).get()
                val elements = document
                    .select("[class=\"hitItem nendoroid nendoroid_series\"], [class=\"hitItem nendoroid_series\"]")
                    .select("div.hitBox>a")
                elements.forEach { ele ->
                    val nendoroid = parseNendoroid(ele, locale)
                    val nendoroidFromDisk = Nendoroid.readNendoroid(nendoroid.num)
                    if (nendoroidFromDisk != null) {
                        nendoroid.merge(nendoroidFromDisk)
                    }
                    Nendoroid.writeNendoroid(nendoroid)
                }
            }
        }

        fun parseByNumber(locale: Locale = JAPANESE) {
            val numSet = mutableListOf<String>()
            for (i in 0..17) {
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
                    val nendoroid = parseNendoroid(ele, locale)
                    val nendoroidFromDisk = Nendoroid.readNendoroid(nendoroid.num)
                    if (nendoroidFromDisk != null) {
                        nendoroid.merge(nendoroidFromDisk)
                    }
                    Nendoroid.writeNendoroid(nendoroid)
                }
            }
        }

        fun parseNendoroid(nendoroid: Nendoroid): Nendoroid {
            val nendoroidJA = parseNendoroid(nendoroid.num, nendoroid.gsc_productNum, JAPANESE)!!
            val nendoroidEN = parseNendoroid(nendoroid.num, nendoroid.gsc_productNum, ENGLISH) ?: return nendoroidJA
            nendoroidJA.merge(nendoroidEN)
            return nendoroidJA
        }

        private fun parseNendoroid(number: String, gsc: Int, locale: Locale): Nendoroid? {
            val URL = "https://www.goodsmile.info/$locale/product/$gsc"
            val keys = mapOf(
                "商品名" to "name", "作品名" to "series", "価格" to "price", "発売時期" to "release", "再販" to "release_info",
                "Product Name" to "name", "Series" to "series", "Price" to "price", "Release Date" to "release"
            )
            val document = try {
                Jsoup.connect(URL).get()
            } catch (e: Exception) {
                return null
            }
            val elements = document.select("div.itemDetail").select("div.detailBox>dl")
            val keyElement = elements.select("dt")
            val valueElement = elements.select("dd")

            val kvMap = mutableMapOf<String, String>()
            for (i in 0 until keyElement.size) {
                val key = keyElement[i].text()
                if (keys.containsKey(key) && !kvMap.containsKey(keys[key])) kvMap[keys[key]!!] = valueElement[i].text()
            }

            val release = mutableSetOf<String>()
            if (locale == JAPANESE) { // 【販売：2017年9月】【再販：2019年5月】【2次再販：2022年10月】
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

        private fun parseNendoroid(element: Element, locale: Locale = JAPANESE): Nendoroid {
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

    class CSV {
        fun parseNendoroid(path: String) {
            val genderMap = mapOf(
                "남" to MALE,
                "여" to FEMALE,
                "남, 여" to MALE_AND_FEMALE,
                "없음" to null,
                "양성" to ANDROGYNY,
                "다요" to DAYO,
                "?" to UNKNOWN,
                "" to null
            )
            val csvParser = loadCSV(path)
            for (csvRecord in csvParser) {
                val data = parseCSV(csvRecord)
                if (data[1] == "") continue
                val nendoroid = Nendoroid(
                    num = data[0],
                    name = mutableMapOf(KOREAN to data[1]),
                    series = mutableMapOf(KOREAN to data[2]),
                    gender = genderMap[data[3]],
                )
                val nendoroidFromDisk = Nendoroid.readNendoroid(data[0])
                if (nendoroidFromDisk == null) {
                    System.err.println("${data[0]} is not on Disk")
                    continue
                }
                nendoroidFromDisk.merge(nendoroid)
                nendoroidFromDisk.writeOnDisk()
            }
        }

        fun parseNendoroidSet(path: String): MutableList<NendoroidSet> {
            val setList = mutableListOf<NendoroidSet>()
            val setMap = mutableMapOf<String, MutableList<String>>()
            val csvParser = loadCSV(path)
            for (csvRecord in csvParser) {
                val data = parseCSV(csvRecord)
                if (data[1] == "" || data[4] == "") continue
                if (!setMap.keys.contains(data[4])) {
                    setMap[data[4]] = mutableListOf(data[0])
                } else {
                    setMap[data[4]]!!.add(data[0])
                }
            }
            setMap.forEach { (key, value) ->
                setList.add(NendoroidSet(key, value))
            }
            return setList
        }

        private fun loadCSV(path: String): CSVParser {
            val file = File(path)
            val br = BufferedReader(FileReader(file))
            return CSVParser(br, CSVFormat.DEFAULT)
        }

        private fun parseCSV(csv: CSVRecord): List<String> {
            //15,14,,,,스즈미야 하루히 바니걸 세트,,스즈미야 하루히의 우울,,여,,"7,333",,스즈미야 하루히의 우울,14
            val number = csv.get(1)
            val numOnly = number.replace("\\D".toRegex(), "").toIntOrNull() ?: -1
            val strOnly = number.replace("\\d".toRegex(), "").replace("dx", "DX")
            val formatted = String.format("%03d", numOnly) + strOnly
            val name = csv.get(5)
            val series = csv.get(7)
            val gender = csv.get(9)
            val setName = csv.get(13)
            return mutableListOf(formatted, name, series, gender, setName)
        }
    }


}
