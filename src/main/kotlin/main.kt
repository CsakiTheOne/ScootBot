import extra.Brainfuck
import extra.EmojiGame
import extra.LolChampions
import extra.adventure.Adventure
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.managers.EmoteManager
import net.dv8tion.jda.internal.entities.EmoteImpl
import java.awt.Color

lateinit var data: Data
lateinit var bot: Bot

fun main() {
    data = Data.load()
    bot = Bot(Secret.getToken("start"))

    setAdminCommands()
    setBasicCommands()
    setBasicTriggers()
    setClickerGame()
    setAdventureGame()
}

fun setAdminCommands() {
    bot.adminCommands["idle toggle"] = {
        bot.getSelf().jda.presence.setStatus(
            if (bot.getSelf().jda.presence.status == OnlineStatus.IDLE) OnlineStatus.ONLINE
            else OnlineStatus.IDLE
        )
        println("Status set to ${bot.getSelf().jda.presence.status.name}")
    }

    bot.adminCommands["guild trust"] = {
        data.trustedGuilds.add(it.guild.id)
        data.save()
        it.channel.sendMessage("Ez jó helynek tűnik, bízok bennetek. :relieved: :heart:").queue()
    }

    bot.adminCommands["guild danger"] = {
        data.trustedGuilds.remove(it.guild.id)
        data.save()
    }

    bot.adminCommands["debug"] = {
        println(it.author.name)
        println(it.author.asTag)
    }

    bot.adminCommands["print"] = {
        println(it.contentRaw)
    }
}

fun setBasicCommands() {
    bot.commands["help"] = {
        val helpMessage = "Parancsok (mindegyik elé `${bot.prefix}`):\n" +
                bot.commands.keys.joinToString() +
                "\n\nKifejezések, amikre reagálok:\n" +
                bot.triggers.keys.joinToString() +
                "\n\nHa idle vagyok, akkor épp dolgoznak a kódomon és nem biztos, hogy mindenre reagálni fogok."
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue()
    }

    bot.commands["ping"] = {
        it.channel.sendMessage(":ping_pong:").queue()
        data.addStat("Ping")
    }

    bot.commands["mondd"] = {
        it.channel.sendMessage("*${it.contentRaw.removePrefix(".mondd ")}*").queue()
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

    bot.commands["brainfuck"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Brainfuck")
                .setDescription(
                    "Bemenet:\n`${it.contentRaw.split(' ')[1]}`\nKimenet:\n`${
                        Brainfuck.run(
                            it.contentRaw.split(
                                ' '
                            )[1]
                        )
                    }`"
                )
                .build()
        ).queue()
    }

    bot.commands["szegz"] = {
        val userFrom = it.author.asMention
        val userTo = it.contentRaw.split(' ')[1]
        it.channel.sendMessage("$userFrom megszegzeli őt: $userTo").queue { msg ->
            msg.addReaction(listOf("❤️", "\uD83D\uDE0F", "\uD83D\uDE1C", "\uD83D\uDE2E").random()).queue()
        }
        data.addStat("Szegz")
    }

    bot.commands["lolchamp"] = {
        val champ = LolChampions.list.random()
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle(champ)
                .setDescription("https://na.leagueoflegends.com/en-us/champions/${champ.toLowerCase()}/")
                .build()
        ).queue()
    }

    bot.commands["emojigame"] = {
        it.channel.sendMessage(EmojiGame.generate()).queue()
        data.addStat("Emoji játék")
    }

    bot.commands["vendégkönyv felír"] = {
        data.guestbook.add(it.author.asTag)
        data.stat["Vendégkönyv"] = data.guestbook.size
        data.save()
        it.channel.sendMessage("Felírtalak a vendégkönyvbe! :book::pen_ballpoint:").queue()
    }

    bot.commands["vendégkönyv töröl"] = {
        data.guestbook.remove(it.author.asTag)
        data.stat["Vendégkönyv"] = data.guestbook.size
        data.save()
        it.channel.sendMessage("Töröltelek a vendégkönyvből").queue()
    }

    bot.commands["vendégkönyv"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Vendégkönyv")
                .setDescription(data.guestbook.joinToString())
                .build()
        ).queue()
    }
}

fun setBasicTriggers() {
    bot.triggers["szeret"] = {
        it.addReaction("❤️").queue()
        data.addStat("Szeretet")
    }

    bot.triggers["yeet"] = {
        it.addReaction("\uD83D\uDCA8").queue()
        data.addStat("Yeet")
    }

    bot.triggers["vices"] = {
        it.addReaction("\uD83D\uDE02").queue()
        data.addStat("Vices")
    }

    bot.triggers["sziasztok"] = {
        val greetings = listOf("Szia!", "Hali!", "Henlo!", "Hey!")
        it.channel.sendMessage(greetings.random()).queue()
    }
}

fun setClickerGame() {
    bot.commands["clicker"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color((0..255).random(), (0..255).random(), (0..255).random()))
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
                    .setColor(Color((0..255).random(), (0..255).random(), (0..255).random()))
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

fun setAdventureGame() {
    bot.commands["adventure"] = {
        if (data.trustedGuilds.contains(it.guild.id)) {
            Adventure.startNew(data, it)
        }
        else {
            it.channel.sendMessage("Ezen a szerveren nem működik a kaland játék.").queue()
        }
    }

    bot.reactionListeners.add {
        it.retrieveMessage().queue { msg ->
            if (msg.embeds[0].title != "Gombóc kaland") return@queue
            Adventure.buttonPressed(data, it, msg)
        }
    }
}