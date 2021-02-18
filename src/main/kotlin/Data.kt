import com.google.gson.Gson
import extra.CustomCommand
import extra.NumGuesser
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Data() {
    var customCommands = mutableListOf<CustomCommand>()
    var numGuesserGames = mutableListOf<NumGuesser>()
    var clickerMessageIds = mutableSetOf<String>()

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./data.json").writeText(dataText)
    }

    companion object {
        val admins = listOf(
            Admin("Csáki", "259610472729280513", "783680267155406868"),
            Admin("Anka", "427127654735413258", "809843289150718042"),
        )

        fun log(sender: String, message: String) {
            File("./log.txt").appendText("${LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY. MM. DD. HH:mm.ss"))} [${sender.toUpperCase()}]: $message\n")
        }

        fun logRead() : String {
            val logs = File("./log.txt").readLines()
            var logText = logs.joinToString("\n")
            var startIndex = 0
            while (logText.length > 1900) {
                startIndex++
                logText = logs.subList(startIndex, logs.size - 1).joinToString("\n")
            }
            return logText

        }

        fun logClear() {
            File("./log.txt").delete()
        }

        fun load() : Data {
            try {
                val gson = Gson()
                val dataText = File("./data.json").readText()
                return gson.fromJson(dataText, Data::class.java)
            }
            catch (ex: Exception) {
                println("Failed to load data!")
            }
            return Data()
        }
    }
}