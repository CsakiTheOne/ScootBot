import com.google.gson.Gson
import extra.NumGuesser
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Data() {
    var numGuesserGames = mutableListOf<NumGuesser>()
    var clickerMessageIds = mutableSetOf<String>()

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./data.json").writeText(dataText)
    }

    companion object {
        val admins = listOf(
            Admin("Csáki", "259610472729280513", "783680267155406868", true),
            Admin("Anka", "427127654735413258", "809843289150718042", true),
        )

        fun log(sender: String, message: String) {
            File("./log.txt").appendText("${LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY. MM. DD. HH:mm.ss"))} [${sender.toUpperCase()}]: $message\n")
        }

        fun logRead() : String {
            val logs = File("./log.txt").readLines()
            val startIndex = if (logs.size < 200) 0 else logs.size - 200
            return logs.subList(startIndex, logs.size - 1).joinToString("\n")
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