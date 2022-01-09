import Bot.Companion.create
import Bot.Companion.makeRemovable
import Bot.Companion.simplify
import Global.Companion.bot
import Global.Companion.data
import Global.Companion.jda
import com.sedmelluq.discord.lavaplayer.player.*
import extra.*
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

lateinit var simpleCommandManager: SimpleCommandManager
var autoActivity = true
var tags = mutableSetOf<String>()

fun reload() {
    data = Data.load()
    Data.logClear()
    simpleCommandManager = SimpleCommandManager.load()

    bot.commands.clear()
    setHelp()
    setAdminCommands()
    setNsfwCommands()
    setBasicCommands()
    //setBasicTriggers()
    setClickerGame()
    setHangmanGame()
    setNumGuesserGame()
    bot.commands.addAll(simpleCommandManager.commands.map { sc -> sc.toCommand() })

    for (cmd in bot.commands) cmd.createTags()
}

fun main() {
    bot = Bot(Secret.getToken("start"))
    RedditAPI.login()
    reload()
    Data.log("Main", "----- BOT STARTED -----")

    Timer().scheduleAtFixedRate(timerTask {
        if (!autoActivity) return@timerTask
        val activities = listOf(
                Activity.playing("${jda.guilds.size} szerveren"),
                Activity.playing("Minecraft"),
                Activity.playing("Terraria"),
                Activity.listening("${Data.prefix}help"),
                Activity.listening("Emerald Hill Zone"),
                Activity.listening("Waterflame"),
                Activity.listening("TheFatRat"),
                Activity.listening("Lindsey Stirling"),
                Activity.watching("Technoblade trolling Skeppy"),
                Activity.watching("Helluva Boss"),
                Activity.watching("🟧⬛"),
        )
        jda.presence.activity = activities.random()
        Data.log("Activity manager", jda.presence.activity.toString())

        //Pinger.pingMinecraftServer(Secret.getMCPort("automatic ping by Timer"))
    }, 3000L, (1000 * 60 * 5).toLong())
}

fun setHelp() {
    Command("help admin", "") {
        val helpMessage = "**Parancsok (prefix: `${Data.prefix}`):**\n" +
                bot.commands.filter { c -> c.isAdminOnly }.map { c -> c.toString() }.sorted().joinToString("\n")
        it.channel.sendMessage(
                EmbedBuilder()
                        .create("Segítség", helpMessage)
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true)

    Command("help játék", "nézd meg milyen játékokat játszhatsz") {
        val helpMessage = "**Játékok (prefix: `${Data.prefix}`):**\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_GAME) }.map { c -> c.toString() }.sorted()
                        .joinToString("\n")
        it.channel.sendMessage(
                EmbedBuilder().create("Játékok", helpMessage)
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("help trigger", "Booba magától reagál dolgokra") {
        val helpMessage = "**Dolgok, amikre Booba önállóan reagálhat:**\n" +
                bot.commands.filter { c -> c.isTrigger }.map { c -> c.toString() }.sorted().joinToString("\n")
        it.channel.sendMessage(
                EmbedBuilder().create("Booba trigger-ek", helpMessage)
                        .setFooter("Regex: <https://regexr.com/>")
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("help", "ÁÁÁÁÁÁÁÁÁ!!!") {
        val helpMessage = "**Parancsok (mindegyik elé `${Data.prefix}`):**\n" +
                "NSFW:\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_NSFW) }.map { c -> c.toString() }.toHashSet().sorted()
                    .joinToString("\n") +
                "\nNORMÁL PARANCSOK:\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_BASIC) && !c.tags.contains(Command.TAG_NSFW) }.map { c -> c.toString() }.toHashSet().sorted()
                        .joinToString("\n")
        it.channel.sendMessage(
                EmbedBuilder().create("Segítség", helpMessage)
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }
}

fun setAdminCommands() {
    Command("reload", "adatok betöltése újra") {
        reload()
        it.channel.sendMessage("Adatok és ${bot.commands.size} (${simpleCommandManager.commands.size} JSON) parancs betöltve. ✅")
                .queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true)

    Command("status offline", "offline-ra állítja a botot") {
        jda.presence.setStatus(OnlineStatus.OFFLINE)
        println("Status set to ${jda.presence.status.name}")
    }.setIsAdminOnly(true)

    Command("activity", "bot állapot állítás") {
        if (it.contentRaw.contains("auto")) autoActivity = true
        else {
            autoActivity = false
            val activityType = it.contentRaw.removePrefix("${Data.prefix}activity ").split(' ')[0]
            jda.presence.activity = when (activityType) {
                "play" -> Activity.playing(it.contentRaw.substring(15))
                "watch" -> Activity.watching(it.contentRaw.substring(16))
                "stream" -> Activity.streaming(it.contentRaw.substring(17), it.contentRaw.substringAfter("URL="))
                "listen" -> Activity.listening(it.contentRaw.substring(17))
                else -> Activity.playing(it.contentRaw.substring(10))
            }
        }
    }.setIsAdminOnly(true)

    Command("log read", "részletes napló olvasása") {
        it.channel.sendMessage("```\n" + Data.logRead() + "\n```").queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true)

    Command("clear", "utolsó üzenetek törlése") {
        val count = it.contentRaw.removePrefix("${Data.prefix}clear ").toInt() + 1
        jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)
        it.channel.history.retrievePast(count).queue { msgs ->
            for (i in 0 until msgs.size) {
                msgs[i].delete().queue {
                    println("[CLEAR]: Deleted message ${i}/${msgs.size - 1}")
                    if (i >= msgs.size - 1) {
                        jda.presence.setStatus(OnlineStatus.ONLINE)
                    }
                }
            }
        }
    }.setIsAdminOnly(true)
}

fun setNsfwCommands() {
    fun createNsfwCmd(head: String, subreddit: String = "") {
        Command(head, if (subreddit.isEmpty()) "" else "(r/$subreddit)") {
            val post = RedditAPI.getRandomPost(if (subreddit.isEmpty()) head else subreddit)
            it.channel.sendMessage("${it.author.asMention} kérésére:\n$post")
                .queue { msg ->
                    msg.makeRemovable()
                }
        }.setIsNSFW(true).addTag(Command.TAG_NSFW)
    }

    createNsfwCmd("ass")
    createNsfwCmd("boobs")
    createNsfwCmd("cute", "TooCuteForPorn")
    createNsfwCmd("extrasmall", "ExtraSmall")
    createNsfwCmd("kneesocks")
    createNsfwCmd("nsfw")
    createNsfwCmd("pear", "PearGirls")
    createNsfwCmd("ratio", "theratio")
    createNsfwCmd("thiccer", "thiccerthanyouthought")
}

fun setBasicCommands() {
    Command("ping", "🏓") {
        it.channel.sendMessage(":ping_pong:").queue { msg -> msg.makeRemovable() }
        //Pinger.pingMinecraftServer(Secret.getMCPort("manual ping with .ping command by ${it.author.asTag}"))
    }

    Command("tagok", "hány ember van ezen a szerveren?") {
        it.guild.loadMembers().onSuccess { members ->
            val csakiStatus =
                    members.firstOrNull { m -> m.id == Data.admins[0].id }?.onlineStatus ?: OnlineStatus.UNKNOWN
            it.channel.sendMessage(
                    EmbedBuilder()
                            .setTitle("Szerver tagok (${members.size})")
                            .setDescription(
                                    "🟢 online: ${members.filter { m -> m.onlineStatus == OnlineStatus.ONLINE }.size} " +
                                            "🤖: ${members.filter { m -> m.user.isBot }.size}\n" +
                                            "🟡 tétlen: ${members.filter { m -> m.onlineStatus == OnlineStatus.IDLE }.size}\n" +
                                            "🔴 elfoglalt: ${members.filter { m -> m.onlineStatus == OnlineStatus.DO_NOT_DISTURB }.size}\n" +
                                            "📡 offline: ${members.filter { m -> m.onlineStatus == OnlineStatus.OFFLINE }.size}\n" +
                                            "Szerver tulaj (${it.guild.owner?.user?.name}): ${it.guild.owner?.onlineStatus}\n" +
                                            "Bot tulaj (${Data.admins[0].name}): $csakiStatus"
                            )
                            .build()
            ).queue { msg -> msg.makeRemovable() }
        }
    }

    Command("mesék", "pár jó mese, amit érdemes nézni") {
        it.channel.sendMessage(
                EmbedBuilder().create("Mesék", "Pár jó mese, amit érdemes nézni.")
                        .addField("Alpha Betas", "VanossGaming YouTube", true)
                        .addField("Disenchantment", "Netflix", true)
                        .addField("Final Space", "Netflix", true)
                        .addField("Helluva Boss", "Vivziepop YouTube", true)
                        .addField("Rick & Moty", "IndaVideo?", true)
                        .addField("Samurai Jack", "?", true)
                        .addField("Sonic Boom", "YouTube", true)
                        .addField("Tapadókorong ember", "YouTube", true)
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("matek", "írj be egy műveletet és kiszámolom neked") {
        if (it.contentRaw == "${Data.prefix}matek") {
            it.channel.sendMessage("Így használd a parancsot: `.matek <művelet>`\nPéldául: `.matek 2 + 2`")
                    .queue { msg -> msg.makeRemovable() }
        } else {
            val jsMathMap = hashMapOf(
                    "sin" to "Math.sin", "cos" to "Math.cos", "tan" to "Math.tan", "pi" to "Math.PI", "sqrt" to "Math.sqrt",
                    "random" to "Math.random()", "rdm" to "Math.random()", "pow" to "Math.pow", "abs" to "Math.abs"
            )
            val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
            var input = it.contentRaw.removePrefix("${Data.prefix}matek ")
            for (pair in jsMathMap) input = input.replace(pair.key, pair.value)
            val ans = engine.eval(input) as Number
            it.channel.sendMessage("$input = $ans").queue { msg -> msg.makeRemovable() }
        }
    }

    Command("js", "tudok futtatni JavaScript kódot") {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
        val input = it.contentRaw.removePrefix("${Data.prefix}js").replace("```js", "")
                .replace("`", "").replace("let", "var").trim()
        val ans = try {
            engine.eval(input) as Any
        } catch (ex: Exception) {
            ex.message
        }
        it.channel.sendMessage(
                EmbedBuilder()
                        .setTitle("JavaScript")
                        .setDescription("```js\n$input\n```\n`> $ans`")
                        .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    /*
    Command("vibe", "vibe-olunk együtt voice-ban?") {
        val vc = it.member?.voiceState?.channel
        if (it.contentRaw.contains("end")) {
            it.guild.audioManager.closeAudioConnection()
        } else if (it.contentRaw == "${Data.prefix}vibe help") {
            it.channel.sendMessage("Vibe-oljunk!\n`.vibe (bababooey|bruh|zsofi|morning|in the 20s|otter|<YouTube link>)`")
                    .queue { msg -> msg.makeRemovable() }
        } else {
            if (vc == null) {
                it.channel.sendMessage("Nem vagy hívásban, szóval nem tudom hová jöjjek.")
                        .queue { msg -> msg.makeRemovable() }
            } else {
                when (val param = it.contentRaw.removePrefix("${Data.prefix}vibe").simplify()) {
                    "bababooey" -> AudioModule.playSound(vc, AudioModule.SOUND_BABABOOEY)
                    "bruh" -> AudioModule.playSound(vc, AudioModule.SOUND_BRUH)
                    "morning" -> AudioModule.playSound(vc, AudioModule.SOUND_MORNING)
                    "in the 20s" -> AudioModule.playSound(vc, AudioModule.SOUND_VIBING20S)
                    "otter" -> AudioModule.playSound(vc, AudioModule.SOUND_OTTER)
                    else -> if (param.isNotEmpty()) AudioModule.playSound(vc, param) else AudioModule.joinVoice(vc)
                }
            }
        }
    }
     */

    Command("emoji kvíz", "találd ki, hogy mit jelentenek az emoji-k") {
        val questions = mapOf(
                "Közmondás: 👀🐮🆕🚪" to "Néz, mint borjú az új kapura.",
                "Közmondás: 🧠⚖💪" to "Többet ésszel, mint erővel.",
                "Közmondás: ❌🏡😺🎼🐭🐭" to "Ha nincs otthon a macska, cincognak az egerek.",
                "Közmondás: ❌👀🌳(🌳🌲🌳🌲)" to "Nem látja a fától az erdőt.",
                "Közmondás: ⌛🔜🌹" to "Türelem rózsát terem",
                "Film: 🧒👴🚗⌚" to "Vissza a jövőbe",
                "Film: 🏰👧👧❄⛄" to "Jégvarázs",
                "Film: 👨👨⚫👽" to "Man In Black",
                "Film: 🕷👦" to "Pókember",
                "Film: 🐢🐁👊🍕" to "Tininindzsa teknőcök",
                "Film: 👦😡💪🟢" to "Hulk",
                "Film: 👧▶🐇⏱🚪 🎩☕ 👑♥👧" to "Alíz Csodaországban",
                "Film: 👧🦁🌹" to "Szépség és a szörnyeteg",
                "Film: ☠🔴🛁" to "Deadpool",
                "Film: 🗽🦁🦒🦓🦛🚢🏝" to "Madagaszkár",
                "Film: 🐁🥄🍲" to "L'ecsó",
                "Márka: 💻📱🍎" to "Apple",
                "Márka: 👟✔" to "Nike",
                "Márka: ⭕x5🚗" to "Audi"
        )

        fun sendEmojiQuiz() {
            var question = questions.keys.random()
            it.channel.sendMessage("$question\n🔃: volt már ❓: megoldás | Következő: `${Data.prefix}emoji kvíz`").queue { msg ->
                msg.addReaction("🔃").queue()
                msg.addReaction("❓").queue()
                bot.reactionListeners.add { event: MessageReactionAddEvent, _: Message ->
                    if (event.messageId == msg.id) {
                        when (event.reactionEmote.emoji) {
                            "🔃" -> {
                                question = questions.keys.random()
                                msg.editMessage(question).queue()
                            }
                            "❓" -> {
                                msg.editMessage(
                                        question + "\nMegoldás: ||" + questions[question] + "||\n" +
                                                "Következő: `${Data.prefix}emoji kvíz`"
                                ).queue()
                                msg.removeReaction("🔃", bot.self).queue()
                                msg.removeReaction("❓", bot.self).queue()
                            }
                        }
                        msg.removeReaction(event.reactionEmote.emoji, event.user!!).queue()
                    }
                }
            }
        }

        when {
            it.contentRaw.contains("méret") -> {
                it.channel.sendMessage("${questions.size}db emoji kvízem van.").queue()
            }
            it.contentRaw.contains("debug") -> {
                it.channel.sendMessage(questions.keys.joinToString("\n")).queue()
            }
            else -> sendEmojiQuiz()
        }
    }.addTag(Command.TAG_GAME)

    Command("hype", "ébreszd fel a szervert reakció gyűjtős játékkal") {
        if (it.contentRaw == "${Data.prefix}hype") {
            it.channel.sendMessage("A parancs használata: `${Data.prefix}hype <szám>`").queue { msg -> msg.makeRemovable() }
        } else {
            lateinit var listener: (e: MessageReactionAddEvent, m: Message) -> Unit
            val max = it.contentRaw.removePrefix("${Data.prefix}hype ").toInt()
            fun onHypeReact(event: MessageReactionAddEvent, msg: Message) {
                if (event.messageId != msg.id) return
                event.retrieveMessage().queue { rmsg ->
                    val n = rmsg.reactions.map { r -> r.count }.sum() + rmsg.emotes.size
                    var percent = (n / max.toFloat() * 100f).toInt()
                    val percentNoLimit = percent
                    if (percent >= 100) percent = 100
                    if (percent < 100) {
                        rmsg.editMessage(
                                "**Hype!** $n/$max Reagálj erre az üzenetre! 🎉\n`8${"=".repeat(percent * 20 / 100)}${
                                    " ".repeat(
                                            20 - (percent * 20 / 100)
                                    )
                                }D` $percentNoLimit%"
                        ).queue()
                    } else {
                        rmsg.editMessage("**Hype!** $n/$max\n$percentNoLimit% 🎉").queue()
                    }
                }
            }
            it.channel.sendMessage("**Hype!** Reagálj erre az üzenetre! 🎉\n`[       START!       ]` ${max * 3} másodpercetek van!")
                    .queue {
                        listener = bot.addReactionListener { event: MessageReactionAddEvent, msg: Message ->
                            onHypeReact(
                                    event,
                                    msg
                            )
                        }
                    }
            Timer().schedule(timerTask {
                bot.reactionListeners.remove(listener)
                it.channel.sendMessage("Hype vége! 🎉 ||Kell egy kis idő a reakciók összeszámolásához, de szép volt!||")
                        .queue()
            }, (max * 3 * 1000).toLong())
        }
    }.addTag(Command.TAG_GAME)
}

fun setBasicTriggers() {
    Command(""".*@random\b.*""", "@random") {
        it.guild.loadMembers().onSuccess { members ->
            val randomMember = members.filter { m ->
                m.onlineStatus != OnlineStatus.OFFLINE && m.onlineStatus != OnlineStatus.DO_NOT_DISTURB &&
                        !m.user.isBot && m.user != it.author
            }.random()
            it.reply(randomMember.asMention).queue()
        }
    }.setIsTrigger(true)

    Command(
            """((szia|helló|hali|csá|cső|hey|henlo) gombóc!*)|sziaszto+k.*|gombóc""",
            "köszönés mindenkinek vagy Gombócnak"
    ) {
        val greetings = listOf("Szia!", "Hali!", "Henlo!", "Hey!", "Heyho!")
        it.channel.sendMessage(greetings.random()).queue()
    }.setIsTrigger(true)

    Command(
            """mit csináltok?.*|ki mit csinál?.*|mit csinálsz gombóc?.*""",
            "mit csináltok? / mit csinálsz Gombóc?"
    ) {
        val activityType = when (jda.presence.activity?.type) {
            Activity.ActivityType.LISTENING -> "Zenét hallgatok 🎧"
            Activity.ActivityType.WATCHING -> "Videót nézek 📽"
            else -> "Játszok 🎮"
        }
        it.channel.sendMessage(activityType).queue()
    }.setIsTrigger(true)

    Command("kő|papír|olló", "kő papír olló") {
        val answers = listOf("Kő \uD83E\uDEA8", "Papír \uD83E\uDDFB", "Olló ✂️")
        it.reply(answers.random()).queue { msg -> msg.makeRemovable() }
    }.setIsTrigger(true).addTag(Command.TAG_GAME)

    Command(".*szeret.*", "szeretet") {
        if (!it.contentRaw.simplify().contains("nem szeret")) {
            it.addReaction("❤️").queue()
        }
    }.setIsTrigger(true)

    Command("""jó {0,1}éj.*""", "jó éjt üzenetek") {
        val greetings = listOf(
                "Aludj jól!",
                "Álmodj szépeket!",
                "Jó éjt!",
                "Jó éjszakát!",
                "Pihend ki magad!",
                "Kitartást holnapra!"
        )
        if (!tags.contains("cooldown_goodnight")) {
            it.channel.sendMessage(greetings.random()).queue()
            tags.add("cooldown_goodnight")
            Timer().schedule(timerTask {
                tags.remove("cooldown_goodnight")
            }, 20000)
        }
    }.setIsTrigger(true)

    Command(
            """.*\b(kibasz|baszdmeg|bazdmeg|fasz|gec|geci|kurva|ribanc|buzi|fuck|rohadj|picsa|picsába|rohadék|cigány).*""",
            "káromkodás"
    ) {
        if (!it.channel.name.simplify().contains("nsfw")) {
            it.addReaction("😠").queue()
            data.diary(
                    "${it.author.asTag} csúnyán beszélt a(z) ${it.channel.name} szobában.\n||${
                        it.contentRaw.replace(
                                "||",
                                "|"
                        )
                    }||"
            )
        }
    }.setIsTrigger(true)

    Command(".*(kapitalizmus|kapitalista).*", "kapitalizmus emlegetése") {
        data.diary("${it.author.asTag} a(z) ${it.channel.name} szobában a ||kapitalizmust|| emlegette.")
    }.setIsTrigger(true)

    Command(""".*\b(csáki).*""", "valaki Csákiról beszél") {
        val guildName =
                if (it.isFromGuild) "**Szerver:** ${it.guild.name} > ${it.channel.name}" else "**Privát:** ${it.channel.name}"
        val embed = EmbedBuilder()
                .setTitle("Említés egy üzenetben (Nem biztos, hogy PONT rólad van szó, csak azt figyelem hogy benne van-e egy bizonyos szöveg az üzenetben)")
                .setDescription("$guildName\n**Üzenet:** ${it.contentRaw}\n**Írta:** ${it.author.asTag}")
                .build()
        jda.getPrivateChannelById(Data.admins[0].privateChannel)?.sendMessage(embed)
                ?.queue { msg -> msg.makeRemovable() }
    }.setIsTrigger(true)
}

fun setClickerGame() {
    Command("clicker", "mint a cookie clicker") {
        it.channel.sendMessage(
                EmbedBuilder()
                        .setColor(Color((0..255).random(), (0..255).random(), (0..255).random()))
                        .setTitle("Clicker játék")
                        .setDescription("🌍: ${data.clicks["global"] ?: 0}\n🖱: 0")
                        .build()
        ).queue { clickerMessage ->
            data.clickerMessageIds.add(clickerMessage.id)
            data.save()
            clickerMessage.addReaction("\uD83D\uDDB1").queue()
            clickerMessage.makeRemovable()
        }
    }.addTag(Command.TAG_GAME)

    bot.reactionListeners.add { event: MessageReactionAddEvent, msg: Message ->
        if (!data.clickerMessageIds.contains(msg.id)) return@add
        if (event.reactionEmote.emoji == "❌") {
            data.clickerMessageIds.remove(msg.id)
            data.save()
            msg.delete().queue()
            return@add
        }
        data.clicks["global"] = (data.clicks["global"] ?: 0) + 1
        data.save()
        val clicks = (msg.embeds[0].description!!.split("\n")[1].split(":")[1].trim().toInt()) + 1
        msg.editMessage(
                EmbedBuilder()
                        .setColor(Color((0..255).random(), (0..255).random(), (0..255).random()))
                        .setTitle("Clicker játék")
                        .setDescription("🌍: ${data.clicks["global"] ?: 0}\n🖱: $clicks")
                        .build()
        ).queue {
            event.reaction.removeReaction(event.user!!).queue()
        }
    }
}

fun setHangmanGame() {
    Command("akasztófa", "G--bóc") {
        if (it.contentRaw == "${Data.prefix}akasztófa") {
            it.channel.sendMessage(
                    EmbedBuilder().setTitle("Akasztófa")
                            .setDescription(
                                    "Statisztikák: `${Data.prefix}akasztófa stat`\n" +
                                            "Új játék: `${Data.prefix}akasztófa ||<szöveg>||` Pl: `${Data.prefix}akasztófa ||gombóc||` vagy " +
                                            "`${Data.prefix}akasztófa ||játék||{❓}`\n" +
                                            "Ezt a vonalat `|` így kell írni: `AltGr + W`\n" +
                                            "Játék módosítók: ❓segítségek"
                            )
                            .build()
            ).queue { msg -> msg.makeRemovable() }
        } else if (it.contentRaw == "${Data.prefix}akasztófa clear") {
            val isPlaying = data.hangmanGames.any { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
            if (isPlaying) {
                data.hangmanGames.removeIf { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
                it.channel.sendMessage("Akasztófa játék törölve :x:").queue { msg -> msg.makeRemovable() }
            } else it.channel.sendMessage("Itt nem volt akasztófa játék.").queue { msg -> msg.makeRemovable() }
        } else if (it.contentRaw.startsWith("${Data.prefix}akasztófa stat")) {
            val embed = EmbedBuilder()
                    .setTitle("Akasztófa statisztikák (2021. 03. 13. óta)")
                    .setDescription("🎮win/játék, 📕szavak, 💀akasztások, +random szó ha van")
            it.guild.loadMembers().onSuccess { members ->
                for (stat in data.hangmanStats) {
                    if (members.any { m -> m.id == stat.playerId }) {
                        embed.addField(jda.getUserById(stat.playerId)?.asTag, stat.toString(), true)
                    }
                }
            }.onError {
                data.hangmanStats.map { hgs ->
                    embed.addField(jda.getUserById(hgs.playerId)?.asTag, hgs.toString(), true)
                }
            }
            it.channel.sendMessage(embed.build()).queue { msg -> msg.makeRemovable() }
        } else {
            if (data.hangmanGames.any { hg -> hg.channelId == it.channel.id }) {
                val lastHangGame = data.hangmanGames.first { hg -> hg.channelId == it.channel.id }
                it.channel.sendMessage("Az előző akasztófa megoldása: ||${lastHangGame.text}||").queue()
                data.hangmanGames.removeIf { hg -> hg.channelId == it.channel.id }
                data.save()
            }
            val param = it.contentRaw.removePrefix("${Data.prefix}akasztófa ").replace("||", "")
                .replace("""\{.*}""".toRegex(), "")
            val mods = if (it.contentRaw.contains('{')) it.contentRaw.split('{')[1].removeSuffix("}") else ""
            it.channel.sendMessage(
                    "**Akasztófa (${it.author.asTag})** Írj egy betűt a kezdéshez!\n```\n${
                        Hangman.toHangedText(param, "")
                    }\n```"
            ).queue { msg ->
                val newGame = Hangman(it.author.id, it.guild.id, it.channel.id, param, mods, msg.id, "")
                data.hangmanGames.add(newGame)
                if (!newGame.toHangedText().contains("-")) {
                    msg.makeRemovable()
                    data.hangmanGames.remove(newGame)
                }
                data.save()
            }
        }
    }.addTag(Command.TAG_GAME)

    Command("""van.*akasztófa\?.*""", "van a szobában akasztófa?") {
        val hangGame = data.hangmanGames.firstOrNull { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
        if (hangGame == null) it.reply("Nem, nincs. Új játékhoz írd be, hogy `${Data.prefix}akasztófa`").queue()
        else hangGame.sendMessage(jda, data, it.channel)
    }.setIsTrigger(true)

    Command("""[a-záéíóöőúüű?]|(a\.[a-záéíóöőúüű?])""", "akasztófa tipp") {
        val hangGame = data.hangmanGames.first { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
        hangGame.guess(it)
    }.setIsTrigger(true)
}

fun setNumGuesserGame() {
    Command("számkitaláló", "gondolok egy számra") {
        if (it.contentRaw == "${Data.prefix}számkitaláló") {
            it.channel.sendMessage(
                    EmbedBuilder()
                            .setTitle("Számkitaláló játékmódok:")
                            .setDescription(
                                    "Alap: `${Data.prefix}számkitaláló <max szám>` pl: `${Data.prefix}számkitaláló 100`\n" +
                                            "Hanna: `${Data.prefix}számkitaláló hanna` (elég nagy kihívás)"
                            )
                            .build()
            ).queue { msg -> msg.makeRemovable() }
        } else {
            val param = it.contentRaw.split(" ")[1]
            val introText: String
            val max = if (param == "hanna") Int.MAX_VALUE else param.toInt()
            val n = if (param == "hanna") (Int.MIN_VALUE..Int.MAX_VALUE).random() else (0..max).random()
            introText =
                    "Gondoltam egy számra **${if (max == Int.MAX_VALUE) Int.MIN_VALUE else 0} és $max** között. Tipphez írd le simán a számot chat-re!"
            it.channel.sendMessage(introText).queue { msg ->
                data.numGuesserGames.add(NumGuesser(it.channel.id, msg.id, n, mutableListOf(param)))
            }
        }
    }.addTag(Command.TAG_GAME)

    Command("-{0,1}[0-9]+", "számkitaláló tipp") {
        val numGuesser = data.numGuesserGames.first { ng -> ng.channelId == it.channel.id }
        val x = it.contentRaw.toInt()
        numGuesser.guess(it, x)
        it.delete().queue()
    }.setIsTrigger(true)
}