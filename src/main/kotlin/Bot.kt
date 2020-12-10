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
                    if (msg.contentRaw == ".debug") {
                        self.jda.presence.setStatus(
                            if (self.jda.presence.status == OnlineStatus.IDLE) OnlineStatus.ONLINE
                            else OnlineStatus.IDLE
                        )
                        println("Status set to ${self.jda.presence.status.name}")
                        msg.delete().queue()
                        return
                    }
                    for (command in commands) {
                        if (msg.contentRaw.startsWith(prefix + command.key) ||
                            msg.contentRaw.startsWith(self.asMention + " " + command.key)) {
                            println("Command received: ${msg.contentRaw} Author: ${msg.author.name}")
                            command.value(msg)
                            msg.delete().queue()
                        }
                    }
                    for (trigger in triggers) {
                        if (trigger.key.toRegex().containsMatchIn(msg.contentRaw.toLowerCase())) {
                            println("Trigger found in message: ${msg.contentRaw} Author: ${msg.author.name}")
                            trigger.value(msg)
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

    fun getSelf() = self

    fun getEmote(guild: Guild, name: String) : Emote {
        return guild.emotes.first { it.name == name }
    }
}