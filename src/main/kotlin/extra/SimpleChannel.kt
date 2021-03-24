package extra

import Global.Companion.jda
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel

class SimpleChannel(
    val guildId: String,
    val channelId: String,
) {
    fun toTextChannel() : TextChannel? {
        return jda.getTextChannelById(channelId)
    }
}