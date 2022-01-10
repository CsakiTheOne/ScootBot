import extra.RedditAPI
import net.dv8tion.jda.api.interactions.components.Button
import java.io.File

class SimpleCommand(
    var head: String,
    var description: String?,
    var actionType: String?,
    var actionText: String?,
    var actionData: String?,
    var isAdminOnly: Boolean?,
    var isNSFW: Boolean?,
    var isTrigger: Boolean?,
    var tags: String?,
) {
    fun toCommand(): Command {
        val cmd = Command(head, description ?: if (actionType == "reddit_random") "r/$actionText" else "") {
            val msg = when (actionType) {
                "message" -> it.channel.sendMessage(actionText ?: "")
                "reply" -> it.reply(actionText ?: "")
                "file" -> it.channel.sendFile(File(actionText ?: ""))
                "reddit_random" -> {
                    val post = RedditAPI.getRandomPost(actionText ?: "")
                    it.channel.sendMessage("${it.author.asMention} kérésére egy poszt r/$actionText sub-ról:\n$post")
                }
                else -> null
            }
            if (actionData?.contains("❌") == true) msg?.setActionRow(Button.primary("close", "❌"))
            msg?.queue()
        }.setIsAdminOnly(isAdminOnly == true).setIsNSFW(isNSFW == true).setIsTrigger(isTrigger == true)
        val tagsList = tags?.split(',')?.map { tag -> tag.trim() } ?: mutableListOf()
        cmd.tags.addAll(tagsList)
        return cmd
    }
}