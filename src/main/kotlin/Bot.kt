import Global.Companion.data
import Global.Companion.jda
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import java.awt.Color

class Bot(token: String) : ListenerAdapter() {
    val commands = mutableListOf<Command>()
    val reactionListeners = mutableListOf<(event: MessageReactionAddEvent, msg: Message) -> Unit>()

    lateinit var self: SelfUser

    init {
        val gatewayIntents = GatewayIntent.getIntents(GatewayIntent.DEFAULT)
        gatewayIntents.add(GatewayIntent.GUILD_MEMBERS)
        gatewayIntents.add(GatewayIntent.GUILD_PRESENCES)
        gatewayIntents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS)
        gatewayIntents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS)
        JDABuilder.create(token, gatewayIntents)
            .addEventListeners(this)
            .build()
    }

    override fun onReady(event: ReadyEvent) {
        jda = event.jda
        self = event.jda.selfUser
        self.jda.openPrivateChannelById(Data.admins[0].id).queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val msg = event.message
        val command = findCommand(event.author, msg.contentRaw) ?: return
        try {
            msg.delete().queue()
        }
        catch (_: Exception) { }
        command.run(msg)
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        if (event.button?.label == "❌") {
            event.message.delete().queue()
            return
        }
        findCommand(event.user, event.button?.id ?: "")?.run(event.message)
        event.deferEdit()
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.isBot == true) return
        val emoji = if (event.reactionEmote.isEmoji) event.reactionEmote.emoji else "custom emote"
        Data.log("MessageReactionListener", emoji)
        when (emoji) {
            "❌" -> {
                event.retrieveMessage().queue { msg ->
                    val isRemovable = msg.reactions.any { react ->
                        (react.isSelf) && react.reactionEmote.emoji == "❌"
                    }
                    if (isRemovable) msg.delete().queue()
                }
            }
            "📌" -> {
                event.retrieveMessage().queue { msg ->
                    msg.removeReaction("❌", self).queue()
                }
            }
            "\uD83D\uDC94" -> {
                event.retrieveMessage().queue { msg ->
                    msg.removeReaction("❤️", self).queue()
                }
            }
        }

        event.retrieveMessage().queue {
            for (reactionListener in reactionListeners) {
                reactionListener(event, it)
            }
        }
    }

    private fun findCommand(author: User, text: String): Command? {
        val content = text.replace("<@!", "<@")
            .replace(self.asMention + " ", Data.prefix)
            .removeSurrounding("||")
        if (Data.admins.any { it.id == author.id } && commands.filter { c -> c.isAdminOnly }.any { c -> c.matches(content) }) {
            return commands.firstOrNull { c -> c.isAdminOnly && c.matches(content) }
        }
        if (commands.filter { c -> c.tags.contains(Command.TAG_BASIC) }.any { c -> c.matches(content) }) {
            return commands.firstOrNull { c -> c.tags.contains(Command.TAG_BASIC) && c.matches(content) }
        }
        if (author.isBot) return null
        return commands.firstOrNull { c -> c.isTrigger && c.matches(content) }
    }

    fun addReactionListener(listener: (event: MessageReactionAddEvent, msg: Message) -> Unit): (event: MessageReactionAddEvent, msg: Message) -> Unit {
        reactionListeners.add(listener)
        return listener
    }

    companion object {
        fun EmbedBuilder?.create(title: String, description: String): EmbedBuilder {
            return EmbedBuilder().setTitle(title).setDescription(description).setColor(Color(0, 128, 255))
        }

        fun String.simplify(): String {
            return this.toLowerCase().trim().replace("á", "a").replace("é", "e")
                .replace("í", "i").replace("ó", "o").replace("ö", "o")
                .replace("ő", "o").replace("ű", "u").replace("ü", "u")
                .replace("ú", "u")
        }
    }
}