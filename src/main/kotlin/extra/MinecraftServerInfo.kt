package extra

class MinecraftServerInfo(
    var description: Description = Description("offline"),
    var players: Players = Players(0, 0),
    var version: Version = Version("0", 0),
) {
    fun isOnline() : Boolean {
        return description.text != "offline"
    }

    class Description(var text: String)
    class Players(var max: Int, var online: Int)
    class Version(var name: String, var protocol: Int)
}