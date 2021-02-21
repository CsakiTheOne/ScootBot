import Bot.Companion.makeRemovable
import Bot.Companion.simplify
import extra.*
import extra.adventure.Adventure
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.awt.Color
import java.util.*
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.concurrent.timerTask

lateinit var data: Data
lateinit var bot: Bot
var autoActivity = true
var tags = mutableSetOf<String>()

fun main() {
    data = Data.load()
    bot = Bot(Secret.getToken("start"))
    Data.logClear()
    Data.log("Main", "----- BOT STARTED -----")

    setHelp()
    setAdminCommands()
    setBasicCommands()
    setBasicTriggers()
    setClickerGame()
    setNumGuesserGame()
    setAdventureGame()

    Timer().scheduleAtFixedRate(timerTask {
        if (!autoActivity) return@timerTask
        val activities = listOf(
            Activity.playing("${bot.getSelf().jda.guilds.size} szerveren"),
            Activity.playing("HealTogether"),
            Activity.playing("Minecraft"),
            Activity.playing("No Man's Sky"),
            Activity.playing("Random Bot"),
            Activity.playing("Sonic Mania"),
            Activity.listening(".help"),
            Activity.listening("Emerald Hill Zone"),
            Activity.listening("Fist Bump"),
            Activity.listening("Lifelight - AmaLee"),
            Activity.listening("TheFatRat"),
            Activity.listening("Lindsey Stirling"),
            Activity.watching("Disenchantment"),
            Activity.watching("TikTok: @csakivevo"),
            Activity.watching("Technoblade trolling Skeppy"),
            Activity.watching("Unusual Memes"),
            Activity.watching("Mr. Robot"),
            Activity.watching("🟧⬛"),
        )
        bot.getSelf().jda.presence.activity = activities.random()
        Data.log("Activity manager", bot.getSelf().jda.presence.activity.toString())
    }, 3000L, 1000 * 60 * 5)
}

fun setHelp() {
    bot.adminCommands["help admin"] = {
        val helpMessage = "**Parancsok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.adminCommands.keys.joinToString()
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["help all"] = {
        val helpMessage = "**Parancsok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.keys.joinToString() +
                "\n\n**Kifejezések, amikre reagálok (regex):**\n" +
                bot.triggers.keys.joinToString().replace(".*", "\\*").replace("|", "/") +
                "\n\nTöbb infó a regex-ről: <https://en.wikipedia.org/wiki/Regular_expression>"
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["help"] = {
        val helpMessage = "**Parancsok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.keys.joinToString()
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["custom"] = {
        if (it.contentRaw == ".custom") {
            val helpMessage = "Új parancs: `.custom add <parancs>; <kimenet>; <egyéb beállítások>`\n" +
                    "Törlés: `.custom delete <parancs>`\nBeállítások: minden emoji egy beállítás\n" +
                    "❌: törölhető üzenet\n\n${data.customCommands.joinToString { cc -> cc.command }}\n" +
                    "**A saját parancsok prefixe:** `..`"
            it.channel.sendMessage(
                EmbedBuilder()
                    .setColor(Color(0, 128, 255))
                    .setTitle("Saját parancsok")
                    .setDescription(helpMessage)
                    .build()
            ).queue { msg -> msg.makeRemovable() }
        }
        else if (it.contentRaw.startsWith(".custom add")) {
            val params = it.contentRaw.removePrefix(".custom add ").split(";").map { param -> param.trim() }
            data.customCommands.add(CustomCommand(params[0], params[1], params[2].contains("❌")))
            data.save()
            it.channel.sendMessage("Új parancs hozzáadva ✅").queue { msg -> msg.makeRemovable() }
        }
        else if (it.contentRaw.startsWith(".custom delete")) {
            data.customCommands.removeIf { cc -> cc.command == it.contentRaw.removePrefix(".custom delete ") }
            data.save()
            it.channel.sendMessage("Parancs törölve ✅").queue { msg -> msg.makeRemovable() }
        }
    }
}

fun setAdminCommands() {
    bot.adminCommands["status toggle"] = {
        bot.getSelf().jda.presence.setStatus(
            if (bot.getSelf().jda.presence.status != OnlineStatus.ONLINE) OnlineStatus.ONLINE
            else OnlineStatus.IDLE
        )
        println("Status set to ${bot.getSelf().jda.presence.status.name}")
    }

    bot.adminCommands["status offline"] = {
        bot.getSelf().jda.presence.setStatus(OnlineStatus.OFFLINE)
        println("Status set to ${bot.getSelf().jda.presence.status.name}")
    }

    bot.adminCommands["activity"] = {
        if (it.contentRaw.contains("auto")) {
            autoActivity = true
        }
        else {
            autoActivity = false
            val activityType = it.contentRaw.removePrefix(".activity ").split(' ')[0]
            bot.getSelf().jda.presence.activity = when (activityType) {
                "play" -> Activity.playing(it.contentRaw.substring(15))
                "watch" -> Activity.watching(it.contentRaw.substring(16))
                "stream" -> Activity.streaming(it.contentRaw.substring(17), it.contentRaw.substringAfter("URL="))
                "listen" -> Activity.listening(it.contentRaw.substring(17))
                else -> Activity.playing(it.contentRaw.substring(10))
            }
        }
    }

    bot.adminCommands["guilds"] = {
        val guilds = bot.getSelf().jda.guilds.joinToString("\n\n") { g -> "**${g.name}** (${g.memberCount})\nTulaj: ${g.owner?.user?.asTag}" }
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Szerver infó")
                .setDescription(guilds)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.adminCommands["log read"] = {
        it.channel.sendMessage("```\n" + Data.logRead() + "\n```").queue { msg -> msg.makeRemovable() }
    }

    bot.adminCommands["log"] = {
        Data.log("Admin", it.contentRaw.removePrefix(".log "))
    }

    bot.adminCommands["wake"] = {
        it.guild.loadMembers().onSuccess { members ->
            val offlines = members.filter { m -> m.onlineStatus == OnlineStatus.OFFLINE }
            val mentions = offlines.joinToString(" ") { m -> m.asMention }
            it.channel.sendMessage("${members.size - offlines.size}/${members.size}\n$mentions").queue { msg -> msg.makeRemovable() }
        }.onError { _ ->
            it.channel.sendMessage("Nem tudom fölkelteni a szervert.").queue { msg -> msg.makeRemovable() }
        }
    }

    bot.adminCommands["clear"] = {
        val count = it.contentRaw.removePrefix(".clear ").toInt() + 1
        bot.getSelf().jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)
        it.channel.history.retrievePast(count).queue { msgs ->
            for (i in 0 until msgs.size) {
                msgs[i].delete().queue {
                    println("[CLEAR]: Deleted message ${i}/${msgs.size - 1}")
                }
            }
        }
        bot.getSelf().jda.presence.setStatus(OnlineStatus.ONLINE)
    }
}

fun setBasicCommands() {
    bot.commands["ping"] = {
        it.channel.sendMessage(":ping_pong:").queue()
    }

    bot.commands["mondd"] = {
        if (Data.admins.any { admin -> admin.id == it.author.id }) {
            it.channel.sendMessage(it.contentRaw.removePrefix(".mondd ")).queue()
        }
        else {
            it.channel.sendMessage("*${it.contentRaw.removePrefix(".mondd ")}*").queue()
        }
    }

    bot.commands["szavazás"] = {
        if (it.contentRaw == ".szavazás") {
            it.channel.sendMessage(
                "Szavazás használata: `.szavazás <kérdés>; <emoji> <válasz>; <emoji2> <válasz2>`\n" +
                    "Például: `.szavazás Hogy vagy?; 👍 Jól!; 👎 Nem a legjobb.`"
            ).queue { msg -> msg.makeRemovable() }
        }
        else {
            val params = it.contentRaw.removePrefix(".szavazás ").split(';').map { r -> r.trim() }
            var options = ""
            for (i in 1 until params.size) {
                options += "${params[i]}\n"
            }
            it.channel.sendMessage(
                EmbedBuilder()
                    .setTitle(params[0])
                    .setDescription(options)
                    .build()
            ).queue { poll ->
                for (i in 1 until params.size) {
                    poll.addReaction(params[i].split(' ')[0]).queue()
                }
            }
        }
    }

    bot.commands["szegz"] = {
        val userFrom = it.author.asMention
        val userTo = it.contentRaw.split(' ')[1]
        it.channel.sendMessage("$userFrom megszegzeli őt: $userTo").queue { msg ->
            msg.addReaction(listOf("❤️", "\uD83D\uDE0F", "\uD83D\uDE1C", "\uD83D\uDE2E").random()).queue()
        }
    }

    bot.commands["gift"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("A wild gift appeared", "https://youtu.be/dQw4w9WgXcQ")
                .setThumbnail("https://static.wikia.nocookie.net/badcreepypasta/images/5/5e/Tumblr_966cc5d6fb3d9ef359cc10f994e26785_e24e7c68_640.png/revision/latest/scale-to-width-down/340?cb=20200229232309")
                .setDescription("```\n     Choccy Milk     \n```")
                .setColor(Color(199, 158, 120))
                .build()
        ).queue()
    }

    bot.commands["lolchamp"] = {
        val champ = LolChampions.list.random()
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle(champ)
                .setDescription("https://na.leagueoflegends.com/en-us/champions/${champ.toLowerCase()}/")
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["matek"] = {
        if (it.contentRaw == ".matek") {
            it.channel.sendMessage("Így használd a parancsot: `.matek <művelet>`\nPéldául: `.matek 2 + 2`")
                .queue { msg -> msg.makeRemovable() }
        }
        else {
            val jsMathMap = hashMapOf(
                "sin" to "Math.sin", "cos" to "Math.cos", "tan" to "Math.tan", "pi" to "Math.PI", "sqrt" to "Math.sqrt",
                "random" to "Math.random()", "rdm" to "Math.random()", "pow" to "Math.pow"
            )
            val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
            val inputRaw = it.contentRaw.removePrefix(".matek ")
            var input = it.contentRaw.removePrefix(".matek ")
            for (pair in jsMathMap) {
                input = input.replace(pair.key, pair.value)
            }
            val ans = engine.eval(input) as Number
            it.channel.sendMessage("$inputRaw = $ans").queue { msg -> msg.makeRemovable() }
        }
    }

    bot.commands["brainfuck"] = {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Brainfuck")
                .setDescription(
                    "Bemenet:\n`${it.contentRaw.split(' ')[1]}`\nKimenet:\n`${
                        Brainfuck.run(it.contentRaw.split(' ')[1])
                    }`"
                )
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["js"] = {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
        val input = it.contentRaw.removePrefix(".js").replace("```js", "")
            .replace("`", "").replace("let", "var").trim()
        val ans = try {
            engine.eval(input) as Any
        }
        catch (ex: Exception) {
            ex.message
        }
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("JavaScript")
                .setDescription("```js\n$input\n```\n`> $ans`")
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    bot.commands["jso"] = {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
        val input = it.contentRaw.removePrefix(".jso").replace("```js", "")
            .replace("`", "").replace("let", "var").trim()
        val ans = try {
            engine.eval(input) as Any
        }
        catch (ex: Exception) {
            ex.message
        }
        it.channel.sendMessage(ans.toString()).queue()
    }

    bot.commands["vibe"] = {
        if (it.contentRaw.contains("end")) {
            it.guild.audioManager.closeAudioConnection()
        }
        else {
            val vc = it.member?.voiceState?.channel
            if (vc == null) {
                it.channel.sendMessage("Nem vagy hívásban, szóval nem tudom hová jöjjek.").queue { msg -> msg.makeRemovable() }
            }
            else {
                it.guild.audioManager.openAudioConnection(vc)
            }
        }
    }

    bot.commands["emoji kvíz"] = {
        val questions = mapOf(
            "Közmondás: 👀🐮🆕🚪" to "Néz, mint borjú az új kapura.", "Közmondás: 🧠⚖💪" to "Többet ésszel, mint erővel.",
            "Közmondás: ❌🏡😺🎼🐭🐭" to "Ha nincs otthon a macska, cincognak az egerek.",
            "Közmondás: ❌👀🌳(🌳🌲🌳🌲)" to "Nem látja a fától az erdőt.", "Közmondás: ⌛🔜🌹" to "Türelem rózsát terem",
            "Film: 🧒👴🚗⌚" to "Vissza a jövőbe", "Film: 🏰👧👧❄⛄" to "Jégvarázs", "Film: 👨👨⚫👽" to "Man In Black",
            "Film: 🕷👦" to "Pókember", "Film: 🐢🐁👊🍕" to "Tininindzsa teknőcök", "Film: 👦😡💪🟢" to "Hulk",
            "Film: 👧▶🐇⏱🚪 🎩☕ 👑♥👧" to "Alíz Csodaországban", "Film: 👧🦁🌹" to "Szépség és a szörnyeteg",
            "Film: ☠🔴🛁" to "Deadpool", "Film: 🗽🦁🦒🦓🦛🚢🏝" to "Madagaszkár", "Film: 🐁🥄🍲" to "L'ecsó",
            "Márka: 💻📱🍎" to "Apple", "Márka: 👟✔" to "Nike", "Márka: ⭕x5🚗" to "Audi"
        )

        fun sendEmojiQuiz() {
            var question = questions.keys.random()
            it.channel.sendMessage("$question 🔃: volt már ❓: megoldás\nKövetkező: `.emoji kvíz`").queue { msg ->
                msg.addReaction("🔃").queue()
                msg.addReaction("❓").queue()
                bot.reactionListeners.add { event ->
                    if (event.messageId == msg.id) {
                        when (event.reactionEmote.emoji) {
                            "🔃" -> {
                                question = questions.keys.random()
                                msg.editMessage(question).queue()
                            }
                            "❓" -> {
                                msg.editMessage(question + "\nMegoldás: ||" + questions[question] + "||\n" +
                                        "Következő: `.emoji kvíz`").queue()
                                msg.removeReaction("🔃", bot.getSelf()).queue()
                                msg.removeReaction("❓", bot.getSelf()).queue()
                            }
                        }
                        msg.removeReaction(event.reactionEmote.emoji, event.user!!).queue()
                    }
                }
            }
        }

        if (it.contentRaw.contains("méret")) {
            it.channel.sendMessage("${questions.size}db emoji kvízem van.").queue()
        }
        else if (it.contentRaw.contains("debug")) {
            it.channel.sendMessage(questions.keys.joinToString("\n")).queue()
        }
        else {
            sendEmojiQuiz()
        }
    }

    bot.commands["emoji ember"] = {
        it.channel.sendMessage(EmojiGame.generate()).queue()
    }

    bot.commands["hype"] = {
        if (it.contentRaw == ".hype") {
            it.channel.sendMessage("A parancs használata: `.hype <szám>`").queue { msg -> msg.makeRemovable() }
        }
        else {
            lateinit var listener: (e: MessageReactionAddEvent) -> Unit
            val max = it.contentRaw.removePrefix(".hype ").toInt()
            fun onHypeReact(event: MessageReactionAddEvent, msg: Message) {
                if (event.messageId != msg.id) return
                event.retrieveMessage().queue { rmsg ->
                    val n = rmsg.reactions.map { r -> r.count }.sum() + rmsg.emotes.size
                    var percent = (n / max.toFloat() * 100f).toInt()
                    val percentNoLimit = percent
                    if (percent >= 100) percent = 100
                    if (percent < 100) {
                        rmsg.editMessage("**Hype!** $n/$max Reagálj erre az üzenetre! 🎉\n`[${"=".repeat(percent * 20 / 100)}${" ".repeat(20 - (percent * 20 / 100))}]` $percentNoLimit%").queue()
                    }
                    else {
                        rmsg.editMessage("**Hype!** $n/$max\n$percentNoLimit% 🎉").queue()
                    }
                }
            }
            it.channel.sendMessage("**Hype!** Reagálj erre az üzenetre! 🎉\n`[       START!       ]` ${max * 5} másodpercetek van!").queue { msg ->
                listener = bot.addReactionListener { event -> onHypeReact(event, msg) }
            }
            Timer().schedule(timerTask {
                bot.reactionListeners.remove(listener)
                it.channel.sendMessage("Hype vége! 🎉").queue()
            }, (max * 3 * 1000).toLong())
        }
    }

    bot.commands["insta"] = {
        it.channel.sendMessage("Az instám: @csicskagombocek").queue { msg -> msg.makeRemovable() }
    }

    bot.commands["fejlesztő"] = {
        it.channel.sendMessage("A készítőm: **@CsakiTheOne#8589** De sokan segítettek jobbá válni ❤").queue()
    }
}

fun setBasicTriggers() {
    bot.triggers[".*@random.*"] = {
        it.guild.loadMembers().onSuccess { members ->
            val randomMember = members.filter { m ->
                (m.onlineStatus == OnlineStatus.ONLINE || m.onlineStatus == OnlineStatus.INVISIBLE) && !m.user.isBot
            }.random()
            it.reply(randomMember.asMention).queue()
        }
    }

    bot.triggers["""((szia|helló|hali|csá|cső|hey|henlo) gombóc!*)|sziaszto+k.*|gombóc"""] = {
        val greetings = listOf("Szia!", "Hali!", "Henlo!", "Hey!", "Heyho!")
        it.channel.sendMessage(greetings.random()).queue()
    }

    bot.triggers["""mit csináltok?.*|ki mit csinál?.*|mit csináls*z* gombóc?.*"""] = {
        val activityType = when (bot.getSelf().jda.presence.activity?.type) {
            Activity.ActivityType.LISTENING -> "Zenét hallgatok 🎧"
            Activity.ActivityType.WATCHING -> "Videót nézek 📽"
            else -> "Játszok 🎮"
        }
        it.channel.sendMessage(activityType).queue()
    }

    bot.triggers[""".*((letölt.*minecraft)|(minecraft.*letölt)).*\?.*"""] = {
        it.reply("A Minecraft-ot innen ajánlom letölteni:\nhttps://tlauncher.org/en/")
            .queue { msg -> msg.makeRemovable() }
    }

    bot.triggers["kő|papír|olló|\uD83E\uDEA8|\uD83E\uDDFB|✂️"] = {
        val answers = listOf("Kő \uD83E\uDEA8", "Papír \uD83E\uDDFB", "Olló ✂️")
        it.reply(answers.random()).queue()
    }

    bot.triggers[".*szeret.*"] = {
        if (!it.contentRaw.simplify().contains("""nem *szeret""".toRegex())) {
            it.addReaction("❤️").queue()
        }
    }

    bot.triggers[".*yeet.*"] = {
        it.addReaction("\uD83D\uDCA8").queue()
    }

    bot.triggers["jó {0,1}reggelt(\\.|!)*"] = {
        it.addReaction("🌄").queue()
    }

    bot.triggers["""jó {0,1}éj.*"""] = {
        val greetings = listOf("Aludj jól!", "Álmodj szépeket!", "Jó éjt!", "Jó éjszakát!", "Pihend ki magad!", "Kitartást holnapra!")
        if (!tags.contains("cooldown_goodnight")) {
            it.channel.sendMessage(greetings.random()).queue()
            tags.add("cooldown_goodnight")
            Timer().schedule(timerTask {
                tags.remove("cooldown_goodnight")
            }, 15000)
        }
    }

    bot.triggers[""".*\b(baszdmeg|bazdmeg|fasz|gec|geci|kurva|ribanc|buzi|fuck|rohadj|picsa|picsába|rohadék).*"""] = {
        it.addReaction("😠").queue()
    }

    bot.triggers[""".*\b(csáki|bius|anka).*"""] = {
        val guildName = if (it.isFromGuild) "**Szerver:** ${it.guild.name} > ${it.channel.name}" else "**Privát:** ${it.channel.name}"
        val embed = EmbedBuilder()
            .setTitle("Említés egy üzenetben (Nem biztos, hogy PONT rólad van szó, csak azt figyelem hogy benne van-e egy bizonyos szöveg az üzenetben)")
            .setDescription("$guildName\n**Üzenet:** ${it.contentRaw}\n**Írta:** ${it.author.asTag}")
            .build()
        bot.getSelf().jda.getPrivateChannelById(Data.admins[0].privateChannel)?.sendMessage(embed)?.queue()
    }

    bot.triggers[""".*\b(bius|anka).*"""] = {
        val guildName = if (it.isFromGuild) "**Szerver:** ${it.guild.name} > ${it.channel.name}" else "**Privát:** ${it.channel.name}"
        val embed = EmbedBuilder()
            .setTitle("Említés egy üzenetben (Nem biztos, hogy PONT rólad van szó, csak azt figyelem hogy benne van-e egy bizonyos szöveg az üzenetben)")
            .setDescription("$guildName\n**Üzenet:** ${it.contentRaw}\n**Írta:** ${it.author.asTag}")
            .build()
        bot.getSelf().jda.getPrivateChannelById(Data.admins[1].privateChannel)?.sendMessage(embed)?.queue()
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
            clickerMessage.makeRemovable()
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
            }
        }
    }
}

fun setNumGuesserGame() {
    bot.commands["számkitaláló"] = {
        val max = if (it.contentRaw.contains(" ")) it.contentRaw.split(" ")[1].toInt() else 100
        it.channel.sendMessage("Gondoltam egy számra 0 és $max között.\nTippelj: `.tipp <szám>`").queue { msg ->
            data.numGuesserGames.add(NumGuesser(it.guild.id, msg.id, (0..max).random()))
        }
    }

    bot.commands["tipp"] = {
        val x = it.contentRaw.removePrefix(".tipp ").toInt()
        val numGuesser = data.numGuesserGames.first { ng -> ng.guildId == it.guild.id }
        when {
            x > numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name}: A gondolt szám kisebb, mint $x.").queue()
                }
            }
            x < numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name}: A gondolt szám nagyobb, mint $x.").queue()
                }
            }
            x == numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name} eltalálta, hogy a szám $x! 🎉").queue {
                        msg.makeRemovable()
                        data.numGuesserGames.remove(numGuesser)
                    }
                }
            }
        }
    }
}

fun setAdventureGame() {
    bot.adminCommands["adventure"] = {
        Adventure.startNew(data, it)
    }

    bot.reactionListeners.add {
        it.retrieveMessage().queue { msg ->
            if (msg.embeds.isNullOrEmpty() || msg.embeds[0].title != "Gombóc kaland") return@queue
            Adventure.buttonPressed(data, it, msg)
        }
    }
}