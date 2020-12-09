import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

fun main() {
    val bot = Bot(Secret.getToken("start"))
    val clickerMessageIds = mutableListOf<String>()

    bot.commands["help"] = {
        val helpMessage = "Parancsok (mindegyik elé `${bot.prefix}`):\n" +
                bot.commands.keys.joinToString() +
                "\n\nKifejezések, amikre reagálok:\n" +
                bot.triggers.joinToString { t -> t.regex }
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue()
    }

    bot.commands["stat"] = {
        val statMessage = bot.triggers.map { t -> "${t.regex}: ${t.getCount()}" }.joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Statisztika")
                .setDescription(statMessage)
                .build()
        ).queue()
    }

    bot.commands["ping"] = {
        it.channel.sendMessage(":ping_pong:").queue()
    }

    bot.commands["clicker"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Clicker játék")
                .setDescription("0")
                .build()
        ).queue { clickerMessage ->
            clickerMessageIds.add(clickerMessage.id)
            clickerMessage.addReaction("\uD83D\uDDB1").queue()
            clickerMessage.addReaction("❌").queue()
        }
    }

    bot.triggers.add(TriggerWord("szeret") {
        it.addReaction("❤️").queue()
    })

    bot.triggers.add(TriggerWord("yeet") {
        it.addReaction("\uD83D\uDCA8").queue()
    })

    bot.reactionListeners.add {
        if (!clickerMessageIds.contains(it.messageId)) return@add
        if (it.reactionEmote.emoji == "❌") {
            it.retrieveMessage().queue { msg ->
                clickerMessageIds.remove(msg.id)
                msg.delete().queue()
            }
            return@add
        }
        it.retrieveMessage().queue { msg ->
            val oldValue = msg.embeds[0].description
            msg.editMessage(
                EmbedBuilder()
                    .setTitle("Clicker játék")
                    .setDescription(((oldValue?.toInt() ?: 0) + 1).toString())
                    .build()
            ).queue { _ ->
                it.reaction.removeReaction(it.user!!).queue()
            }
        }
    }
}
