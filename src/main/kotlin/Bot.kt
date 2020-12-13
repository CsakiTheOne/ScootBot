import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent

class Bot(token: String) {
    var prefix = "."
    val adminCommands = mutableMapOf<String, (msg: Message) -> Unit>()
    val commands = mutableMapOf<String, (msg: Message) -> Unit>()
    val triggers = mutableMapOf<String, (msg: Message) -> Unit>()
    val reactionListeners = mutableListOf<(e: MessageReactionAddEvent) -> Unit>()

    private lateinit var self: SelfUser

    init {
        val gatewayIntents = GatewayIntent.getIntents(GatewayIntent.DEFAULT)
        gatewayIntents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS)
        gatewayIntents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS)
        JDABuilder.create(token, gatewayIntents)
            .addEventListeners(object: ListenerAdapter() {
                override fun onReady(event: ReadyEvent) {
                    self = event.jda.selfUser
                }
                override fun onMessageReceived(event: MessageReceivedEvent) {
                    val msg = event.message
                    val content = msg.contentRaw.replace("<@!", "<@")
                    if (!msg.isFromGuild) {
                        Data.log("Bot", "Private message from ${msg.author.name}: $content")
                    }
                    for (aCommand in adminCommands) {
                        if (content.startsWith(prefix + aCommand.key) ||
                            content.startsWith(self.asMention + " " + aCommand.key)) {
                            if (msg.author.id != "259610472729280513") break
                            Data.log("Bot", "Admin command received: $content Author: ${msg.author.name} (${msg.author.id})")
                            aCommand.value(msg)
                            msg.delete().queue()
                            break
                        }
                    }
                    for (command in commands) {
                        if (content.startsWith(prefix + command.key) ||
                            content.startsWith(self.asMention + " " + command.key)) {
                            Data.log("Bot", "Command received: $content Author: ${msg.author.name} (${msg.author.id})")
                            command.value(msg)
                            msg.delete().queue()
                            break
                        }
                    }
                    for (trigger in triggers) {
                        if (trigger.key.toRegex().matches(content.toLowerCase())) {
                            Data.log("Bot", "Trigger found in message: $content Author: ${msg.author.name} (${msg.author.id})")
                            trigger.value(msg)
                            break
                        }
                    }
                }
                override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
                    if (event.user?.isBot == true) return
                    if (event.reactionEmote.emoji == "❌") {
                        event.retrieveMessage().queue { msg ->
                            val isRemovable = msg.reactions.any { react ->
                                (react.isSelf) && react.reactionEmote.emoji == "❌"
                            }
                            if (isRemovable) msg.delete().queue()
                        }
                    }
                    for (reactionListener in reactionListeners) {
                        reactionListener(event)
                    }
                }
            })
            .setActivity(Activity.listening("you ❤"))
            .build()
    }

    fun getSelf() = self

    companion object {
        fun Message?.makeRemovable(callback: (() -> Unit)? = null) {
            this?.addReaction("❌")?.queue { callback?.invoke() }
        }
    }
}