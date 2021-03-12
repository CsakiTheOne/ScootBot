import Bot.Companion.makeRemovable
import net.dv8tion.jda.api.entities.Message

class Command(val head: String, val description: String, private val action: (msg: Message) -> Unit) {
    var isAdminOnly: Boolean = false
    var isNSFW: Boolean = false
    var tags: MutableList<String> = mutableListOf()

    fun run(msg: Message) : Boolean {
        if (isAdminOnly && !Data.admins.any { a -> a.id == msg.author.id }) {
            msg.channel.sendMessage("Ezt a parancsot csak egy gombóc admin használhatja!").queue { it.makeRemovable() }
            return false
        }
        if (isNSFW && !msg.textChannel.isNSFW) {
            msg.channel.sendMessage("Ezt a parancsot csak NSFW szobában lehet használni!").queue { it.makeRemovable() }
            return false
        }
        Data.log("Bot", "Command received: ${msg.contentRaw} Author: ${msg.author.asTag} (${msg.author.id})")
        action(msg)
        return true
    }

    fun setIsAdminOnly(value: Boolean) : Command {
        isAdminOnly = value
        return this
    }

    fun setIsNSFW(value: Boolean) : Command {
        isNSFW = value
        return this
    }

    fun addTag(value: String) : Command {
        tags.add(value)
        return this
    }

    fun isThisCommand(text: String) : Boolean {
        return text.startsWith("${Data.prefix}$head")
    }

    override fun toString(): String {
        return "$head - $description"
    }

    companion object {
        val TAG_GAME = "game"
    }
}