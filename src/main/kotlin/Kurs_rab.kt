@file:Suppress("KotlinConstantConditions")

package sem2.kurs_rab

import com.mongodb.client.MongoDatabase
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.apache.log4j.helpers.Loader.getResource
import org.json.JSONObject
import org.litote.kmongo.*
import kotlin.system.exitProcess

@Serializable
data class Coordinates(
    @SerialName("Неделя") val typeOfWeeK: String,
    @SerialName("День") val dayOfWeek: String,
    @SerialName("Идентификатор") val id: Int
)

@Serializable
data class Participants(
    @SerialName("Преподаватель") val teacher: String? = null,
    @SerialName("Группа") val group: String? = null,
    @SerialName("Подгруппа") val subgroup: Int? = null
)

@Serializable
data class ScheduleMongo(
    @SerialName("День и Неделя") val coordinates: Coordinates,
    @SerialName("Время") val time: String,
    @SerialName("Номер пары") val numberClass: Int,
    @SerialName("Участники") val participants: Participants,
    @SerialName("Предмет") val discipline: String? = null,
    @SerialName("Тип занятия") val type: String? = null,
    @SerialName("Аудитория") val classroom: String? = null
)

fun bdInList(id: List<Int>): ArrayList<List<ScheduleMongo>> {
    var idCount = 0
    val objectArray = ArrayList<List<ScheduleMongo>>()
    while (idCount <= id.lastIndex) {
        val objectSingle = scheduleMongo.find(ScheduleMongo::coordinates / Coordinates::id eq id[idCount]).toList()
        objectArray.add(objectSingle)
        idCount++
    }
    return objectArray
}

fun transfer(
    bdArray: ArrayList<List<ScheduleMongo>>,
    BeforeTransfer: List<ScheduleMongo>,
    transfer: List<ScheduleMongo>
) {

    val idBefore = BeforeTransfer.map { it.coordinates.id }[0]
    val dayBefore = BeforeTransfer.map { it.coordinates.dayOfWeek }[0]
    val weekBefore = BeforeTransfer.map { it.coordinates.typeOfWeeK }[0]
    val timeBefore = BeforeTransfer.map { it.time }[0]
    val numberBefore = BeforeTransfer.map { it.numberClass }[0]
    val idTransfer = transfer.map { it.coordinates.id }[0]
    val dayTransfer = transfer.map { it.coordinates.dayOfWeek }[0]
    val weekTransfer = transfer.map { it.coordinates.typeOfWeeK }[0]
    val timeTransfer = transfer.map { it.time }[0]
    val numberTransfer = transfer.map { it.numberClass }[0]
    val bdBefore = bdArray[idBefore - 1]
    val bdTransfer = bdArray[idTransfer - 1]
    val conditionFind = scheduleMongo.find(ScheduleMongo::coordinates / Coordinates::id eq idBefore + 1).toList()
    val condition = conditionFind.map { it.discipline }
    val conditionInt = conditionFind.map { it.numberClass }
    if ((condition.isEmpty() || condition[0] == null) || (condition.isNotEmpty() && conditionInt[0] == 1)) {
        bdArray[idBefore - 1] = bdTransfer
        bdArray[idTransfer - 1] = bdBefore
        scheduleMongo.deleteMany()
        var insertCount = 0
        while (insertCount <= bdArray.lastIndex) {
            scheduleMongo.insertMany(bdArray[insertCount])
            insertCount++
        }
        scheduleMongo.updateMany(
            ScheduleMongo::coordinates / Coordinates::id eq idBefore,
            arrayListOf(
                setValue(ScheduleMongo::coordinates / Coordinates::dayOfWeek, dayTransfer),
                setValue(ScheduleMongo::coordinates / Coordinates::typeOfWeeK, weekTransfer),
                setValue(ScheduleMongo::time, timeTransfer),
                setValue(ScheduleMongo::numberClass, numberTransfer),
            )
        )
        scheduleMongo.updateOne(
            ScheduleMongo::coordinates / Coordinates::id eq idTransfer,
            arrayListOf(
                setValue(ScheduleMongo::coordinates / Coordinates::dayOfWeek, dayBefore),
                setValue(ScheduleMongo::coordinates / Coordinates::typeOfWeeK, weekBefore),
                setValue(ScheduleMongo::time, timeBefore),
                setValue(ScheduleMongo::numberClass, numberBefore),
            )
        )
        prettyPrintCursor(scheduleMongo.find())
        println("Перенос успешно проведен")
    } else println("Перенос невозможен. Появляется окно.")
}

fun prettyPrintJson(json: String) =
    println(
        JSONObject(json)
            .toString(4)
    )

fun prettyPrintCursor(cursor: Iterable<*>) =
    prettyPrintJson("{ result: ${cursor.json} }")

val client = KMongo
    .createClient("mongodb://root:cWFC0cJ6sqfH@192.168.75.160:27017")
val mongoDatabase: MongoDatabase = client.getDatabase("курсовая")
val scheduleMongo = mongoDatabase.getCollection<ScheduleMongo>().apply { drop() }


fun main() {
    val scheduleJson = getResource("kursovay.json").readText()
    val scheduleCol: List<ScheduleMongo> = Json { ignoreUnknownKeys = true }.decodeFromString(
        ListSerializer(ScheduleMongo.serializer()),
        scheduleJson
    )
    println(scheduleCol.size)
    scheduleMongo.insertMany(scheduleCol)
    val findAll = scheduleMongo.find()
    val id = findAll.map { it.coordinates.id }.toList()
    val bdArray = bdInList(id)
    println("Введите пару для переноса (Название дисциплины):")
    val para = readln()
    println("Введите тип пары для переноса (Лек, Лаб, КСР, Пр):")
    val tip = readln()
    println("Введите номер пары для переноса (1-5):")
    val number = readln().toInt()
    println("Введите неделю где расположена пара (Четная, Нечетная):")
    var week = readln()
    val lesson = scheduleMongo.find(
        and(
            ScheduleMongo::discipline eq para,
            ScheduleMongo::type eq tip,
            ScheduleMongo::numberClass eq number,
            ScheduleMongo::coordinates / Coordinates::typeOfWeeK eq week

        )
    ).toList()
    prettyPrintCursor(lesson)
    println("Выберите неделю для переноса (Четная, Нечетная)")
    week = readln()
    val weekFind = scheduleMongo.find(
        and(
            ScheduleMongo::coordinates / Coordinates::typeOfWeeK eq week,
            ScheduleMongo::discipline eq null
        )
    )
    prettyPrintCursor(weekFind)
    println("Выберите день и номер пары куда переносить (введите идентификатор)")
    println("Для отмены оставьте поле пустым")
    val idTransfer = readln()
    if (idTransfer != "") {
        val idTransferInt = idTransfer.toInt()
        val transfer = scheduleMongo.find(ScheduleMongo::coordinates / Coordinates::id eq idTransferInt).toList()
        transfer(bdArray, lesson, transfer)
    } else exitProcess(1)
}