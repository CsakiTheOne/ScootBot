import com.google.gson.Gson
import extra.Hangman
import extra.NumGuesser
import extra.SimpleChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Data() {
    var diaryChannel = SimpleChannel("", "")

    var clickerMessageIds = mutableSetOf<String>()
    var clicks = mutableMapOf<String, Int>()

    var hangmanGames = mutableListOf<Hangman>()
    var hangmanStats = mutableListOf<Hangman.PlayerStats>()
    var numGuesserGames = mutableListOf<NumGuesser>()

    fun addHangmanStat(stat: Hangman.PlayerStats) {
        if (hangmanStats.any { s -> s.playerId == stat.playerId }) {
            hangmanStats.first { s -> s.playerId == stat.playerId }.add(stat)
        } else hangmanStats.add(stat)
    }

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./data.json").writeText(dataText)
    }

    fun diary(text: String, callback: ((Message) -> Unit)? = null) {
        diaryChannel.toTextChannel()?.sendMessage(text)?.queue {
            it.crosspost().queue()
            callback?.invoke(it)
        }
    }

    fun diary(embed: MessageEmbed, callback: ((Message) -> Unit)? = null) {
        diaryChannel.toTextChannel()?.sendMessage(embed)?.queue {
            it.crosspost().queue()
            callback?.invoke(it)
        }
    }

    companion object {
        val prefix = "."

        val admins = listOf(
            Admin("Csáki", "259610472729280513", "783680267155406868"),
            Admin("Anka", "427127654735413258", "809843289150718042"),
            Admin("hopelight", "521357031391756291", "818914164550533130"),
        )

        var log = ""

        fun log(sender: String, message: String) {
            log += "${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY. MM. dd. HH:mm.ss"))
            } [${sender.toUpperCase()}]: $message\n"
        }

        fun logRead(): String {
            var startIndex = 0
            val logs = log.split("\n")
            while (log.length > 1900) {
                startIndex++
                log = logs.subList(startIndex, logs.size - 1).joinToString("\n")
            }
            return log

        }

        fun logClear() {
            log = ""
        }

        fun load(): Data {
            try {
                val gson = Gson()
                val dataText = File("./data.json").readText()
                return gson.fromJson(dataText, Data::class.java)
            } catch (ex: Exception) {
                println("Failed to load data!")
            }
            return Data()
        }
    }
}