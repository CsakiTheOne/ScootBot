package extra

import Bot.Companion.makeRemovable
import net.dv8tion.jda.api.entities.Message

class NumGuesser(
    val channelId: String,
    val messageId: String,
    val num: Int,
    val tags: MutableList<String> = mutableListOf(),
) {
    fun guess(msg: Message, x: Int, onWin: () -> Unit) {
        msg.channel.retrieveMessageById(messageId).queue { _ ->
            when {
                x > num -> {
                    var newContent = "${msg.contentRaw}\n`${msg.author.name}: X < $x`"
                    if (newContent.length > 1000) newContent += " (kb. ${2000 - newContent.length} karakter maradt)"
                    msg.channel.editMessageById(messageId, newContent).queue()
                }
                x < num -> {
                    var newContent = "${msg.contentRaw}\n`${msg.author.name}: X > $x`"
                    if (newContent.length > 1000) newContent += " (kb. ${2000 - newContent.length} karakter maradt)"
                    msg.channel.editMessageById(messageId, newContent).queue()
                }
                x == num -> {
                    msg.channel.editMessageById(
                        messageId,
                        "${msg.contentRaw}\n${msg.author.name} eltalálta, hogy a szám $x! 🎉\nÚj játék: `.számkitaláló`"
                    ).queue { edited ->
                        edited.makeRemovable()
                        onWin()
                    }
                }
            }
        }
        msg.delete().queue()
    }
}