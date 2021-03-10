import net.dv8tion.jda.api.entities.Message

class Command(val head: String, val description: String, val action: (msg: Message) -> Unit) {
    var isAdminOnly: Boolean = false
    var tags: MutableList<String> = mutableListOf()

    fun setIsAdminOnly(value: Boolean) : Command {
        isAdminOnly = value
        return this
    }

    fun addTag(value: String) : Command {
        tags.add(value)
        return this
    }

    override fun toString(): String {
        return "$head - $description"
    }

    companion object {
        val TAG_GAME = "game"
    }
}