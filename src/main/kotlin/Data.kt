import com.google.gson.Gson
import java.io.File
import java.lang.Exception

class Data() {
    var clickerMessageIds = mutableListOf<String>()
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