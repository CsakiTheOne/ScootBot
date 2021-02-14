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
        gatewayIntents.add(GatewayIntent.GUILD_MEMBERS)
        gatewayIntents.add(GatewayIntent.GUILD_PRESENCES)
        gatewayIntents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS)
        gatewayIntents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS)
        JDABuilder.create(token, gatewayIntents)
            .addEventListeners(object: ListenerAdapter() {
                override fun onReady(event: ReadyEvent) {
                    self = event.jda.selfUser
                }
                override fun onMessageReceived(event: MessageReceivedEvent) {
                    val msg = event.message
                    val content = msg.contentRaw.replace("<@!", "<@").replace(self.asMention + " ", ".")
                    if (msg.author.isBot) return
                    if (!msg.isFromGuild) {
                        Data.log("Bot", "Private message from ${msg.author.asTag} (Channel id: ${msg.channel.id}): $content")
                    }
                    for (aCommand in adminCommands) {
                        if (content.startsWith(prefix + aCommand.key) && Data.admins.any { it.id == msg.author.id }) {
                            Data.log("Bot", "Admin command received: $content Author: ${msg.author.asTag} (${msg.author.id})")
                            aCommand.value(msg)
                            msg.delete().queue()
                            break
                        }
                    }
                    for (command in commands) {
                        if (content.startsWith(prefix + command.key)) {
                            Data.log("Bot", "Command received: $content Author: ${msg.author.asTag} (${msg.author.id})")
                            command.value(msg)
                            msg.delete().queue()
                            break
                        }
                    }
                    for (trigger in triggers) {
                        if (trigger.key.simplify().toRegex().matches(content.simplify())) {
                            Data.log("Bot", "Trigger found in message: $content Author: ${msg.author.asTag} (${msg.author.id})")
                            trigger.value(msg)
                            break
                        }
                    }
                }
                override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
                    if (event.user?.isBot == true) return
                    val emoji = if (event.reactionEmote.isEmoji) event.reactionEmote.emoji else "custom emote"
                    Data.log("MessageReactionListener", emoji)
                    if (emoji == "❌") {
                        event.retrieveMessage().queue { msg ->
                            val isRemovable = msg.reactions.any { react ->
                                (react.isSelf) && react.reactionEmote.emoji == "❌"
                            }
                            if (isRemovable) msg.delete().queue()
                        }
                    }
                    else if (emoji == "\uD83D\uDC94") {
                        event.retrieveMessage().queue { msg ->
                            msg.removeReaction("❤️", self).queue()
                        }
                    }
                    for (reactionListener in reactionListeners) {
                        reactionListener(event)
                    }
                }
            })
            .build()
    }

    fun getSelf() = self

    fun addReactionListener(listener: (e: MessageReactionAddEvent) -> Unit) : (e: MessageReactionAddEvent) -> Unit {
        reactionListeners.add(listener)
        return listener
    }

    companion object {
        fun Message?.makeRemovable(callback: (() -> Unit)? = null) {
            this?.addReaction("❌")?.queue { callback?.invoke() }
        }

        fun String.simplify() : String {
            return this.toLowerCase().trim().replace("á", "a").replace("e", "é")
                .replace("í", "i").replace("ó", "o").replace("ö", "o")
                .replace("ő", "o").replace("ű", "u").replace("ü", "u")
                .replace("ú", "u")
        }
    }
}