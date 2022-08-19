import data.*
import extensions.load
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


val parser = Parser()
const val basePath = "../NendoroidDB/Nendoroid"
fun main() {
    parser.init() // 폴더 생성
    parser.copyPreJSON() // 파싱이 제한되는 넨도들 복사
    // 각각 페이지에 누락되는 케이스들이 있어 겹치는 케이스가 많으나 각각 수행함.
    // 그럼에도 불구하고 없는 번호 : 1522-DX, 1694, 1695, 1855, 1871, 1872
    parser.gsc.parseByNumber(locale = Locale.ENGLISH) // 번호별 페이지 파싱
    parser.gsc.parseByYear(locale = Locale.ENGLISH) // 년도별 페이지 파싱
    parser.gsc.parseByNumber() // 번호별 페이지 파싱
    parser.gsc.parseByYear() // 년도별 페이지 파싱
    parser.csv.parseNendoroid("parsed.csv") // 넨겔 시트 DB 부분 추출
    updateNendoroids() // 공홈 각 넨도별 페이지 파싱
    updateSetInfo()
    checkAllDB()
}

fun updateSetInfo() {
    val setList = parser.csv.parseNendoroidSet("parsed.csv")
    var serial = 1
    setList.forEach { set ->
        set.num = serial.toString()
        set.save()
        set.list.forEach { number ->
            val nendoroid: Nendoroid? = Nendoroid(number).load()
            if (nendoroid != null) {
                nendoroid.addSetInfo(serial)
                nendoroid.save()
            }
        }
        serial++
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
        val parsedNendoroid = parser.gsc.parseNendoroid(diskNendoroid)
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

fun checkAllDB() {
    val nendoroidList = getAllNendoroids()
    val list = Collections.synchronizedList(ArrayList<String>())
    nendoroidList.parallelStream().forEach {
        val dn:Nendoroid = Nendoroid(it.nameWithoutExtension).load() ?: return@forEach
        if (dn.set == 0) {
            list.add(dn.num)
        }
    }
    list.sorted().forEach {
        println(it)
    }
}

private fun getAllNendoroids(path: String = ""): ArrayList<File> {
    val nendoroidList = ArrayList<File>()
    val path = File(path.ifEmpty { "NendoroidDB/Nendoroid" })
    val fList = path.listFiles()
    fList.forEach {
        if (it.isDirectory && it.name != "Set") {
            nendoroidList.addAll(getAllNendoroids(it.path))
            return@forEach
        }
        nendoroidList.add(it)
    }
    return nendoroidList
}