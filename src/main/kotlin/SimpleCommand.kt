import Bot.Companion.makeRemovable
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
    fun toCommand() : Command {
        val cmd = Command(head, description ?: "") {
            when (actionType) {
                "message" -> it.channel.sendMessage(actionText ?: "")
                    .queue { msg -> if (actionData?.contains("❌") == true) msg.makeRemovable() }
                "reply" -> it.reply(actionText ?: "")
                    .queue { msg -> if (actionData?.contains("❌") == true) msg.makeRemovable() }
                "file" -> it.channel.sendFile(File(actionText ?: ""))
                    .queue { msg -> if (actionData?.contains("❌") == true) msg.makeRemovable() }
            }
        }.setIsAdminOnly(isAdminOnly == true).setIsNSFW(isNSFW == true).setIsTrigger(isTrigger == true)
        val tagsList = tags?.split(',')?.map { tag -> tag.trim() } ?: mutableListOf()
        cmd.tags.addAll(tagsList)
        return cmd
    }
}