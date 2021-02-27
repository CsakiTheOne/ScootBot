import com.google.gson.Gson
import extra.CustomCommand
import extra.NumGuesser
import extra.SimpleChannel
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Data() {
    var diaryChannel = SimpleChannel("", "")
    var customCommands = mutableListOf<CustomCommand>()
    var clickerMessageIds = mutableSetOf<String>()
    var clicks = mutableMapOf<String, Int>()

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./data.json").writeText(dataText)
    }

    fun diary(jda: JDA, text: String, callback: ((Message) -> Unit)? = null) {
        diaryChannel.toTextChannel(jda)?.sendMessage(text)?.queue(callback)
    }

    fun diary(jda: JDA, embed: MessageEmbed, callback: ((Message) -> Unit)? = null) {
        diaryChannel.toTextChannel(jda)?.sendMessage(embed)?.queue(callback)
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