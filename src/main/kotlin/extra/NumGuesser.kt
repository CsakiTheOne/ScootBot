package extra

class NumGuesser(
    val guildId: String,
    val channelId: String,
    val messageId: String,
    val num: Int,
    val tags: MutableList<String> = mutableListOf(),
)