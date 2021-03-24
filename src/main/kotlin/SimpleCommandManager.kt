import com.google.gson.Gson
import java.io.File
import java.lang.Exception

class SimpleCommandManager {
    var commands: MutableList<SimpleCommand> = mutableListOf()

    fun save() {
        val gson = Gson()
        val dataText = gson.toJson(this)
        File("./commands.json").writeText(dataText)
    }

    companion object {
        fun load(): SimpleCommandManager {
            try {
                val gson = Gson()
                val dataText = File("./commands.json").readText()
                return gson.fromJson(dataText, SimpleCommandManager::class.java)
            } catch (ex: Exception) {
                println("Failed to load commands!")
            }
            return SimpleCommandManager()
        }
    }
}