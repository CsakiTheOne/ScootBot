import com.google.gson.Gson
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Message
import java.io.File
import java.lang.Exception
import java.sql.Time
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Data() {
    var guestbook = mutableSetOf<String>()
    var trustedGuilds = mutableSetOf<String>()
    var clickerMessageIds = mutableSetOf<String>()
    var stat = mutableMapOf<String, Int>()

    fun addStat(name: String, value: Int = 1) {
        stat[name] = (stat[name]?: 0) + value
        save()
    }

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./data.json").writeText(dataText)
    }

    companion object {
        fun log(sender: String, message: String) {
            File("./log.txt").appendText("${LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY. MM. DD. HH:mm.ss"))} [${sender.toUpperCase()}]: $message\n")
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