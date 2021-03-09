import net.dv8tion.jda.api.entities.Message

class Command(val head: String, val description: String, val action: (msg: Message) -> Unit) {
    var isAdminOnly: Boolean = false

    fun setIsAdminOnly(value: Boolean) : Command {
        isAdminOnly = value
        return this
    }
}