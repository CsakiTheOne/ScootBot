import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.awt.Color

class Bot(token: String) {
    val prefix = "."
    val commands = mutableMapOf<String, (msg: Message) -> Unit>()
    val triggers = mutableListOf<TriggerWord>()
    val reactionListeners = mutableListOf<(e: MessageReactionAddEvent) -> Unit>()

    init {
        val gatewayIntents = GatewayIntent.getIntents(GatewayIntent.DEFAULT)
        gatewayIntents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS)
        gatewayIntents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS)
        JDABuilder.create(token, gatewayIntents)
            .addEventListeners(object: ListenerAdapter() {
                override fun onMessageReceived(event: MessageReceivedEvent) {
                    val msg = event.message
                    for (command in commands) {
                        if (msg.contentRaw.startsWith(prefix + command.key)) {
                            command.value(msg)
                            msg.delete().queue()
                        }
                    }
                    for (trigger in triggers) {
                        if (trigger.regex.toRegex().containsMatchIn(msg.contentRaw.toLowerCase())) {
                            trigger.execute(msg)
                        }
                    }
                }
                override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
                    if (event.user?.isBot == true) return
                    for (reactionListener in reactionListeners) {
                        reactionListener(event)
                    }
                }
            })
            .setActivity(Activity.listening("you ❤"))
            .build()
    }
}