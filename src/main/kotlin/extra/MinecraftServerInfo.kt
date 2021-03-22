package extra

import com.google.gson.Gson

class MinecraftServerInfo(
    var description: Description = Description("offline"),
    var players: Players = Players(0, 0),
    var version: Version = Version("0", 0),
) {
    fun isOnline() : Boolean {
        return description.text != "offline"
    }

    fun simplify() : String {
        return Gson().toJson(Simplified(description.text, players.online, players.max, version.name))
            .replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .replace(",maxPlayers:", "/")
            .replace(",", "\n")
    }

    class Description(var text: String)
    class Players(var max: Int, var online: Int)
    class Version(var name: String, var protocol: Int)
    class Simplified(var motd: String, var players: Int, var maxPlayers: Int, var version: String)
}