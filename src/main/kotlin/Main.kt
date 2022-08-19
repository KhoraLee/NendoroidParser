import utils.CSVParser
import utils.GSCParser
import utils.Parser
import java.util.*

const val basePath = "../NendoroidDB/Nendoroid"
fun main() {
    val csv = CSVParser("parsed.csv")
    val gsc = GSCParser
    Parser.createBaseDir() // 폴더 생성
    Parser.copyPreJSON() // 파싱이 제한되는 넨도들 복사
    // 각각 페이지에 누락되는 케이스들이 있어 겹치는 케이스가 많으나 각각 수행함.
    // 그럼에도 불구하고 없는 번호 : 1522-DX, 1694, 1695, 1855, 1871, 1872
    gsc.parseByNumber(locale = Locale.ENGLISH) // 번호별 페이지 파싱
    gsc.parseByYear(locale = Locale.ENGLISH) // 년도별 페이지 파싱
    gsc.parseByNumber() // 번호별 페이지 파싱
    gsc.parseByYear() // 년도별 페이지 파싱
    csv.updateNendoroids() // 넨겔 시트 DB 부분 추출
    gsc.updateNendoroids() // 공홈 각 넨도별 페이지 파싱
    csv.updateSetInfo()
}
