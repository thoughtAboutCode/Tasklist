package tasklist

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType

val tasks = mutableListOf<Task>()

val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
val type: ParameterizedType = Types.newParameterizedType(List::class.java, Task::class.java, TaskPriority::class.java)
val taskListAdapter: JsonAdapter<List<Task?>> = moshi.adapter(type)
val jsonFile = File("tasklist.json")

data class Task(
        var task: List<String> = emptyList(),
        var priority: TaskPriority = TaskPriority.NORMAL,
        var taskDate: String = "",
        var taskTime: String = ""
) {
    fun calculateDueTag(): DueTag {
        val theTaskDate = taskDate.toLocalDate()
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val numberOfDays = currentDate.daysUntil(theTaskDate)
        return when {
            numberOfDays == 0 -> DueTag.TODAY
            numberOfDays > 0 -> DueTag.IN_TIME
            else -> DueTag.OVERDUE
        }
    }
}

enum class UserInput(val input: String) {
    ADD("add"), PRINT("print"), END("end"), DELETE("delete"), EDIT("edit")
}

enum class TaskPriority(val tag: Char, val color: String) {
    CRITICAL('C', "\u001B[101m \u001B[0m"), HIGH('H', "\u001B[103m \u001B[0m"), NORMAL(
            'N',
            "\u001B[102m \u001B[0m"
    ),
    LOW('L', "\u001B[104m \u001B[0m")
}

enum class DueTag(val tag: Char, val color: String) {
    IN_TIME('I', "\u001B[102m \u001B[0m"), TODAY('T', "\u001B[103m \u001B[0m"), OVERDUE('O', "\u001B[101m \u001B[0m")
}

fun taskPriority(): TaskPriority {
    var priority: String
    do {
        println("Input the task priority (C, H, N, L):")
        priority = readln().trim()
    } while (priority.length != 1 ||
            priority.first().uppercaseChar() !in enumValues<TaskPriority>().map(TaskPriority::tag)
    )
    return enumValues<TaskPriority>().first { current -> current.tag == priority.first().uppercaseChar() }
}

fun taskDate(): String {
    var taskDate: LocalDate?
    do {
        println("Input the date (yyyy-mm-dd):")
        taskDate = try {
            readln().trim().split("-").mapIndexed { index, value ->
                if (index == 0) value.padStart(4, '0')
                else value.padStart(2, '0')
            }.joinToString(separator = "-").toLocalDate()
        } catch (ex: Exception) {
            println("The input date is invalid")
            null
        }
    } while (taskDate == null)

    return taskDate.toString()
}

fun taskTime(date: String): String {
    var taskTime: String?
    do {
        println("Input the time (hh:mm):")
        taskTime = try {
            readln().trim().split(":").joinToString(separator = ":") { value ->
                value.padStart(2, '0')
            }.also {
                with(it.split(":")){
                    if (size != 2) throw IllegalArgumentException()
                    date.toLocalDate().atTime(first().toInt(), last().toInt())
                }
            }
        } catch (ex: Exception) {
            println("The input time is invalid")
            null
        }
    } while (taskTime == null)

    return taskTime
}

fun taskContent(): List<String>? {
    println("Input a new task (enter a blank line to end):")
    val task = buildList {
        do {
            val input = readln().trim()
        } while (input.isNotBlank().also { if (it) add(input) })
    }

    if (task.isEmpty()) {
        println("The task is blank")
        return null
    }
    return task
}

fun addTask() {
    val newTask = Task()
    with(newTask) {
        priority = taskPriority()
        taskDate = taskDate()
        taskTime = taskTime(taskDate)
        task = taskContent() ?: return
    }
    tasks.add(newTask)
}

fun printTasks() {
    if (tasks.isEmpty()) {
        println("No tasks have been input")
        return
    }
    printTableHeader()
    tasks.forEachIndexed { index, task ->
        task.task.forEachIndexed { lineIndex, line ->
            val lineCutTime = line.length / 44
            val lineCutRemainder = line.length % 44
            printTableOneLineContent(
                    taskNumOrder = if (lineIndex == 0) (index + 1).toString() else "",
                    taskDate = if (lineIndex == 0) task.taskDate else "",
                    taskTime = if (lineIndex == 0) task.taskTime else "",
                    taskPriorityColor = if (lineIndex == 0) task.priority.color else " ",
                    taskDueTagColor = if (lineIndex == 0) task.calculateDueTag().color else " ",
                    taskLineContent = line.substring(0..if (lineCutTime > 0) 43 else line.lastIndex)
            )
            if (lineCutTime > 1) {
                repeat(lineCutTime - 1) {
                    printTableOneLineContent(
                            taskLineContent = line.substring((44 * (it + 1)) until (44 * (it + 1)) + 44)
                    )
                }
            }
            if (lineCutTime > 0 && lineCutRemainder > 0)
                printTableOneLineContent(
                        taskLineContent = line.substring((44 * lineCutTime) until (44 * lineCutTime) + lineCutRemainder)
                )
        }
        printFullHorizontalLine()
    }
}

fun printTableHeader() {
    printFullHorizontalLine()
    println("| N  |    Date    | Time  | P | D |                   Task                     |")
    printFullHorizontalLine()
}

fun printTableOneLineContent(
        taskNumOrder: String = "",
        taskDate: String = "",
        taskTime: String = "",
        taskPriorityColor: String = " ",
        taskDueTagColor: String = " ",
        taskLineContent: String = ""
) {
    println(
            "| ${taskNumOrder.padEnd(2, ' ')} | ${taskDate.padEnd(10, ' ')} | ${
                taskTime.padEnd(
                        5,
                        ' '
                )
            } | $taskPriorityColor | $taskDueTagColor |${taskLineContent.padEnd(44, ' ')}|"
    )
}

fun printFullHorizontalLine() =
        println("+${"-".repeat(4)}+${"-".repeat(12)}+${"-".repeat(7)}+${"-".repeat(3)}+${"-".repeat(3)}+${"-".repeat(44)}+")

fun chooseTask(): Int {
    printTasks()
    var taskNumberToDelete: Int
    do {
        println("Input the task number (1-${tasks.size}):")
        taskNumberToDelete = try {
            readln().toInt()
        } catch (ex: Exception) {
            -1
        }
    } while ((taskNumberToDelete !in 1..tasks.size).also { if (it) println("Invalid task number") })
    return taskNumberToDelete
}

fun deleteTask() {
    if (tasks.isEmpty()) {
        println("No tasks have been input")
    } else {
        val taskNumberToDelete = chooseTask()
        tasks.removeAt(taskNumberToDelete - 1)
        println("The task is deleted")
    }
}

fun editTask() {
    if (tasks.isEmpty()) {
        println("No tasks have been input")
    } else {
        val taskNumberToEdit = chooseTask()
        do {
            println("Input a field to edit (priority, date, time, task):")
            when (readln()) {
                "priority" -> tasks[taskNumberToEdit - 1].priority = taskPriority()
                "date" -> tasks[taskNumberToEdit - 1].taskDate = taskDate()
                "time" -> tasks[taskNumberToEdit - 1].taskTime = taskTime(tasks[taskNumberToEdit - 1].taskDate)
                "task" -> tasks[taskNumberToEdit - 1].task = taskContent() ?: break
                else -> {
                    println("Invalid field")
                    continue
                }
            }
            println("The task is changed")
            break
        } while (true)
    }
}

fun handleUserInput(userInput: UserInput): UserInput {
    when (userInput) {
        UserInput.ADD -> addTask()
        UserInput.PRINT -> printTasks()
        UserInput.END -> {
            saveTaskList()
            println("Tasklist exiting!")
        }

        UserInput.DELETE -> deleteTask()
        UserInput.EDIT -> editTask()
    }
    return userInput
}

fun uploadTask() {
    if (jsonFile.exists()) {
        tasks.addAll(taskListAdapter.fromJson(jsonFile.readText())?.filterNotNull() ?: emptyList())
    }
}

fun saveTaskList() {
    if (tasks.isNotEmpty()) {
        if (jsonFile.exists()) jsonFile.delete()
        jsonFile.createNewFile()
        jsonFile.writeText(taskListAdapter.toJson(tasks))
    }
}

fun userInputValidation(): String {
    lateinit var userInput: String
    do {
        println("Input an action (add, print, edit, delete, end):")
        userInput = readln()
    } while ((userInput !in enumValues<UserInput>().map(UserInput::input)).also {
                if (it) {
                    println("The input action is invalid")
                }
            })
    return userInput
}

fun askUser() {
    do {
        val userInputString = userInputValidation()
        val userInput = handleUserInput(enumValues<UserInput>().first { it.input == userInputString })
    } while (userInput != UserInput.END)
}

fun main() {
    uploadTask()
    askUser()
}