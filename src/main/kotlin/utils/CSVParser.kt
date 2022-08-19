package utils

import data.Gender
import data.Nendoroid
import data.NendoroidSet
import extensions.load
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

class CSVParser(private val path: String) {

    private val genderMap = mapOf(
        "남" to Gender.MALE,
        "여" to Gender.FEMALE,
        "남, 여" to Gender.MALE_AND_FEMALE,
        "없음" to null,
        "양성" to Gender.ANDROGYNY,
        "다요" to Gender.DAYO,
        "?" to Gender.UNKNOWN,
        "" to null
    )

    fun updateNendoroids() {
        val csv = loadCSV()
        for (record in csv) {
            val data = parseCSV(record)
            if (data[1] == "") continue // if name is empty
            val nendoroid = Nendoroid(
                num = data[0],
                name = mutableMapOf(Locale.KOREAN to data[1]),
                series = mutableMapOf(Locale.KOREAN to data[2]),
                gender = genderMap[data[3]],
            )
            val nendoroidFromDisk: Nendoroid? = Nendoroid(data[0]).load()
            if (nendoroidFromDisk == null) {
                System.err.println("${data[0]} is not on Disk")
                continue
            }
            nendoroidFromDisk.merge(nendoroid)
            nendoroidFromDisk.save()
        }
    }

    fun updateSetInfo() {
        val setList = getNendoroidSets()
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

    private fun getNendoroidSets(): MutableList<NendoroidSet> {
        val setList = mutableListOf<NendoroidSet>()
        val setMap = mutableMapOf<String, MutableList<String>>()
        val csv = loadCSV()
        for (record in csv) {
            val data = parseCSV(record)
            if (data[1] == "" || data[4] == "") continue // if name or setName is empty
            if (!setMap.keys.contains(data[4])) {
                setMap[data[4]] = mutableListOf(data[0])
            } else {
                setMap[data[4]]!!.add(data[0])
            }
        }
        setMap.forEach { (key, value) ->
            setList.add(NendoroidSet(setName = key, list = value))
        }
        return setList
    }

    private fun loadCSV(): CSVParser {
        val file = File(path)
        val br = BufferedReader(FileReader(file))
        return CSVParser(br, CSVFormat.DEFAULT)
    }

    private fun parseCSV(record: CSVRecord): List<String> {
        //15,14,,,,스즈미야 하루히 바니걸 세트,,스즈미야 하루히의 우울,,여,,"7,333",,스즈미야 하루히의 우울,14
        val number = record.get(0)
        val numOnly = number.replace("\\D".toRegex(), "").toIntOrNull() ?: -1
        val strOnly = number.replace("\\d".toRegex(), "").replace("dx", "DX")
        val formatted = String.format("%03d", numOnly) + strOnly
        val name = record.get(1)
        val series = record.get(2)
        val gender = record.get(3)
        val setName = record.get(4)
        return mutableListOf(formatted, name, series, gender, setName)
    }

}