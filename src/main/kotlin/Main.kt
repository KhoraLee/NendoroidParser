import Data.Nendoroid
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    val parser = Parser()
    parser.init() // 폴더 생성
    // 각각 페이지에 누락되는 케이스들이 있어 겹치는 케이스가 많으나 각각 수행함.
    // 그럼에도 불구하고 없는 번호 : 1522-DX, 1694, 1695, 1871, 1872
    parser.gsc.parseByNumber() // 번호별 페이지 파싱
    parser.gsc.parseByYear() // 년도별 페이지 파싱
    parser.csv.parseNendoroid("sheet.csv") // 넨겔 시트 DB 부분 추출
    updateNendoroids() // 공홈 각 넨도별 페이지 파싱
//    checkAllDB()
}

fun updateNendoroids() {
    val parser = Parser()
    val nendoroidList = getAllNendoroids()
    val total = nendoroidList.size
    val current = AtomicInteger()
    nendoroidList.parallelStream().forEach {
        current.incrementAndGet()
        if (it.nameWithoutExtension == "1538") return@forEach // 특설페이지로 파싱 불가
        val diskNendoroid = Nendoroid.readNendoroid(it.nameWithoutExtension) ?: return@forEach
        val parsedNendoroid = parser.gsc.parseNendoroid(diskNendoroid)
        diskNendoroid.merge(parsedNendoroid)
        diskNendoroid.writeOnDisk()
        println(
            """[${String.format("%04d", current.get())} / $total]
                | ${diskNendoroid.num}\t\t
                | (Thread : " + Thread.currentThread().name + ")""".trimMargin()
        )
        Thread.sleep(50)
    }
}

fun checkAllDB() {
    val nendoroidList = getAllNendoroids()
    val list = Collections.synchronizedList(ArrayList<String>())
    nendoroidList.parallelStream().forEach { it ->
        val dn = Nendoroid.readNendoroid(it.nameWithoutExtension) ?: return@forEach
        if (dn.price == -1 || dn.price == 0) {
            list.add(dn.num)
        }
    }
    list.sorted().forEach {
        println(it)
    }
}

private fun getAllNendoroids(path: String = ""): ArrayList<File> {
    val nendoroidList = ArrayList<File>()
    val path = File(path.ifEmpty { "D:\\NendoroidDB\\Nendoroid" })
    val fList = path.listFiles()
    fList.forEach {
        if (it.isDirectory) {
            nendoroidList.addAll(getAllNendoroids(it.path))
            return@forEach
        }
        nendoroidList.add(it)
    }
    return nendoroidList
}

