import Bot.Companion.makeRemovable
import net.dv8tion.jda.api.entities.Message

class Command(val head: String, val description: String, private val action: (msg: Message) -> Unit) {
    var isAdminOnly: Boolean = false
    var isNSFW: Boolean = false
    var isTrigger: Boolean = false
    var doIgnoreCase: Boolean = false
    var tags: MutableSet<String> = mutableSetOf()

    fun createTags() {
        if (!isAdminOnly && !isTrigger) tags.add(TAG_BASIC)
    }

    fun run(msg: Message): Boolean {
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

    fun setIsAdminOnly(value: Boolean): Command {
        isAdminOnly = value
        return this
    }

    fun setIsNSFW(value: Boolean): Command {
        isNSFW = value
        return this
    }

    fun setIsTrigger(value: Boolean): Command {
        isTrigger = value
        if (value) doIgnoreCase = true
        return this
    }

    fun setIgnoreCase(value: Boolean): Command {
        doIgnoreCase = value
        return this
    }

    fun addTag(value: String): Command {
        tags.add(value)
        return this
    }

    fun matches(text: String): Boolean {
        val input = if (doIgnoreCase) text.toLowerCase() else text
        return if (isTrigger) head.toRegex().matches(input)
        else input.startsWith("${Data.prefix}$head")
    }

    override fun toString(): String {
        if (isTrigger) return "$description ||`$head`||"
        return ".$head - $description"
    }

    companion object {
        val TAG_BASIC = "basic"
        val TAG_GAME = "game"
    }
}