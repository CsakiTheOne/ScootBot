import net.dv8tion.jda.api.entities.Message

class TriggerWord(
    val regex: String,
    val action: (msg: Message) -> Unit
) {
    private var count = 0

    fun getCount() = count

    fun execute(msg: Message) {
        count++
        action(msg)
    }
}