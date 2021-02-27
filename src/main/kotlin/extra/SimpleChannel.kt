package extra

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel

class SimpleChannel(
    val guildId: String,
    val channelId: String,
) {
    fun toTextChannel(jda: JDA) : TextChannel? {
        return jda.getTextChannelById(channelId)
    }
}