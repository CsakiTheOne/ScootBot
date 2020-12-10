import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

fun main() {
    val data = Data.load()
    val bot = Bot(Secret.getToken("start"))

    bot.commands["help"] = {
        val helpMessage = "Parancsok (mindegyik elé `${bot.prefix}`):\n" +
                bot.commands.keys.joinToString() +
                "\n\nKifejezések, amikre reagálok:\n" +
                bot.triggers.keys.joinToString()
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue()
    }

    bot.commands["stat reset"] = {
        data.stat = mutableMapOf()
        data.save()
    }

    bot.commands["stat"] = {
        val statMessage = if (data.stat.isNullOrEmpty())
            "Üres"
        else
            data.stat.map { s -> "${s.key}: ${s.value}" }.joinToString("\n")
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
        data.addStat("Ping")
    }

    bot.commands["brainfuck"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                    .setTitle("Brainfuck")
                    .setDescription("Bemenet:\n`${it.contentRaw.split(' ')[1]}`\nKimenet:\n`${Brainfuck.run(it.contentRaw.split(' ')[1])}`")
                .build()
        ).queue()
    }

    bot.commands["clicker"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Clicker játék")
                .setDescription("0")
                .build()
        ).queue { clickerMessage ->
            data.clickerMessageIds.add(clickerMessage.id)
            data.save()
            clickerMessage.addReaction("\uD83D\uDDB1").queue()
            clickerMessage.addReaction("❌").queue()
        }
    }

    bot.triggers["szeret"] = {
        it.addReaction("❤️").queue()
        data.addStat("Szeretet")
    }

    bot.triggers["yeet"] = {
        it.addReaction("\uD83D\uDCA8").queue()
        data.addStat("Yeet")
    }

    bot.reactionListeners.add {
        if (!data.clickerMessageIds.contains(it.messageId)) return@add
        if (it.reactionEmote.emoji == "❌") {
            it.retrieveMessage().queue { msg ->
                data.clickerMessageIds.remove(msg.id)
                data.save()
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
                data.addStat("Click")
            }
        }
    }
}
