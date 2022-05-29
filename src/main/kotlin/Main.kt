import Data.Nendoroid
import java.io.File

fun main() {
    val parser = Parser()
    parser.init() // 폴더 생성
    // 각각 페이지에 누락되는 케이스들이 있어 겹치는 케이스가 많으나 각각 수행함.
    // 그럼에도 불구하고 없는 번호 : 1522-DX, 1694, 1695, 1871, 1872
    parser.gsc.parseByNumber() // 번호별 페이지 파싱
    parser.gsc.parseByYear() // 년도별 페이지 파싱
    parser.csv.parseNendoroid("sheet.csv") // 넨겔 시트 DB 부분 추출
    updateNendoroids() // 공홈 각 넨도별 페이지 파싱
    checkAllDB()
}

fun updateNendoroids(path: String = "") {
    val parser = Parser()

    val path = File(path.ifEmpty { "D:\\NendoroidDB\\Nendoroid" })
    val fList: Array<File> = path.listFiles()

    fList.forEach {
        if (it.isDirectory) {
            updateNendoroids(it.path)
            return@forEach
        }
        if (it.nameWithoutExtension == "1538") return@forEach // 특설페이지로 파싱 불가
        val diskNendoroid = Nendoroid.readNendoroid(it.nameWithoutExtension) ?: return@forEach
        val parsedNendoroid = parser.gsc.parseNendoroid(diskNendoroid)
        diskNendoroid.merge(parsedNendoroid)
        diskNendoroid.writeOnDisk()
        print("${it.nameWithoutExtension}\t")
    }
    println()
}

fun checkAllDB(path: String = "") {
    val sb = StringBuilder()
    val path = File(path.ifEmpty { "D:\\NendoroidDB\\Nendoroid" })
    val fList: Array<File> = path.listFiles()

    sb.append("번호,이름,시리즈,이름_KO,시리즈_KO\n")

    fList.forEach { it ->
        if (it.isDirectory) {
            sb.append(checkAllDB(it.path))
            return@forEach
        }
        val dn = Nendoroid.readNendoroid(it.nameWithoutExtension) ?: return@forEach
        dn.release_date.forEach { str ->
            if (str.length != 7){
                println(str)
            }
        }
    }

}
