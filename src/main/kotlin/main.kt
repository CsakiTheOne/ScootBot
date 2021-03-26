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
    setBasicCommands()
    setBasicTriggers()
    setClickerGame()
    setHangmanGame()
    setNumGuesserGame()
    bot.commands.addAll(simpleCommandManager.commands.map { sc -> sc.toCommand() })

    for (cmd in bot.commands) cmd.createTags()
}

fun main() {
    bot = Bot(Secret.getToken("start"))
    reload()
    Data.log("Main", "----- BOT STARTED -----")

    Timer().scheduleAtFixedRate(timerTask {
        if (!autoActivity) return@timerTask
        val activities = listOf(
            Activity.playing("${jda.guilds.size} szerveren"),
            Activity.playing("Minecraft"),
            Activity.playing("Terraria (köszönöm @Krey)"),
            Activity.listening(".help"),
            Activity.listening("Emerald Hill Zone"),
            Activity.listening("Lifelight - AmaLee"),
            Activity.listening("Thunderstruck - AC/DC"),
            Activity.listening("TheFatRat"),
            Activity.listening("Lindsey Stirling"),
            Activity.watching("TikTok: @csakivevo"),
            Activity.watching("Technoblade trolling Skeppy"),
            Activity.watching("Minecraft stream online órán"),
            Activity.watching("Final Space"),
            Activity.watching("🟧⬛"),
        )
        jda.presence.activity = activities.random()
        Data.log("Activity manager", jda.presence.activity.toString())

        Pinger.pingMinecraftServer()
    }, 3000L, (1000 * 60 * 5).toLong())
}

fun setHelp() {
    Command("help admin", "") {
        val helpMessage = "**Parancsok (prefix: `${Data.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> c.isAdminOnly }.map { c -> c.toString() }.sorted().joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder()
                .create("Gombóc segítség", helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true)

    Command("help játék", "nézd meg milyen játékokat játszhatsz") {
        val helpMessage = "**Játékok (prefix: `${Data.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_GAME) }.map { c -> c.toString() }.sorted()
                    .joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder().create("Gombóc játékok", helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("help trigger", "Gombóc magától reagál dolgokra") {
        val helpMessage = "**Dolgok, amikre Gombóc önállóan reagál:**\n" +
                bot.commands.filter { c -> c.isTrigger }.map { c -> c.toString() }.sorted().joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder().create("Gombóc trigger-ek", helpMessage)
                .setFooter("Regex: <https://regexr.com/>")
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("help", "ÁÁÁÁÁÁÁÁÁ!!!") {
        val helpMessage = "**Parancsok (mindegyik elé `${Data.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_BASIC) }.map { c -> c.toString() }.sorted()
                    .joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder().create("Gombóc segítség", helpMessage)
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
            val activityType = it.contentRaw.removePrefix(".activity ").split(' ')[0]
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
        val count = it.contentRaw.removePrefix(".clear ").toInt() + 1
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

fun setBasicCommands() {
    Command("ping", "🏓") {
        it.channel.sendMessage(":ping_pong:").queue { msg -> msg.makeRemovable() }
        Pinger.pingMinecraftServer()
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
                                "Gombóc tulaj (${Data.admins[0].name}): $csakiStatus"
                    )
                    .build()
            ).queue { msg -> msg.makeRemovable() }
        }
    }

    Command("mondd", "ki tudsz mondatni velem valamit") {
        it.channel.sendMessage(
            if (Data.admins.any { admin -> admin.id == it.author.id }) it.contentRaw.removePrefix(".mondd ")
            else "*${it.contentRaw.removePrefix(".mondd ")}*"
        ).queue()
    }

    Command("számláló", "számolok dolgokat") {
        when (it.contentRaw) {
            ".számláló" -> {
                it.channel.sendMessage(
                    EmbedBuilder()
                        .setTitle("Számoló")
                        .setDescription("`.számoló lista` `.számoló <valami> 1` `.számoló <valami> -1` `.számoló <valami> reset`")
                        .build()
                ).queue { msg -> msg.makeRemovable() }
            }
            ".számláló lista" -> {
                it.channel.sendMessage(
                    EmbedBuilder()
                        .setTitle("Számoló")
                        .setDescription(data.counters.keys.joinToString { k -> "$k: ${data.counters[k]}" })
                        .build()
                ).queue { msg -> msg.makeRemovable() }
            }
            else -> {
                val params = it.contentRaw.split(' ')
                val counter = params[1]
                val modifier = if (params.size > 2) params[2] else ""
                if (modifier == "reset") data.counters.remove(counter)
                else if ("-?[0-9]+".toRegex().matches(modifier)) data.counters[counter] =
                    (data.counters[counter] ?: 0) + modifier.toInt()

                if (data.counters[counter] == 0) data.counters.remove(counter)

                it.channel.sendMessage("$counter: ${data.counters[counter] ?: 0}").queue { msg ->
                    msg.addReaction("👆").queue()
                    msg.addReaction("👇").queue()
                    msg.makeRemovable()
                }
            }
        }
        data.save()
    }

    bot.reactionListeners.add { event: MessageReactionAddEvent, msg: Message ->
        val key = msg.contentRaw.split(':')[0]
        if (data.counters.any { c -> c.key == key }) {
            when (event.reactionEmote.emoji) {
                "👆" -> {
                    data.counters[key] = (data.counters[key] ?: 0) + 1
                    msg.removeReaction("👆", event.user!!).queue()
                }
                "👇" -> {
                    data.counters[key] = (data.counters[key] ?: 0) - 1
                    msg.removeReaction("👇", event.user!!).queue()
                }
            }
            msg.editMessage("$key: ${data.counters[key] ?: 0}").queue()
        }
    }

    Command("szavazás", "én egy demokratikusan megválasztott bot vagyok") {
        if (it.contentRaw == ".szavazás") {
            it.channel.sendMessage(
                "Szavazás használata: `.szavazás <kérdés>; <emoji> <válasz>; <emoji2> <válasz2>`\n" +
                        "Például: `.szavazás Hogy vagy?; 👍 Jól!; 👎 Nem a legjobb.`"
            ).queue { msg -> msg.makeRemovable() }
        } else {
            val params = it.contentRaw.removePrefix(".szavazás ").split(';').map { r -> r.trim() }
            val options = params.subList(1, params.size - 1).joinToString("\n")
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

    Command("szegz", "nagyon romi") {
        val userFrom = it.author.asMention
        val userTo = it.contentRaw.split(' ')[1]
        it.channel.sendMessage("$userFrom megszegzeli őt: $userTo").queue { msg ->
            msg.addReaction(listOf("❤️", "\uD83D\uDE0F", "\uD83D\uDE1C", "\uD83D\uDE2E").random()).queue()
        }
    }.setIsNSFW(true)

    Command("mesék", "pár jó mese, amit érdemes nézni") {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("Mesék")
                .setDescription("Pár jó mese, amit érdemes nézni.")
                .addField("Alpha Betas", "On YouTube from VanossGaming", true)
                .addField("Disenchantment", "Netflix", true)
                .addField("Final Space", "Netflix", true)
                .addField("Helluva Boss", "On YouTube from Vivziepop", true)
                .addField("Samurai Jack", "", true)
                .addField("Sonic Boom", "On YouTube", true)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }

    Command("matek", "írj be egy műveletet és kiszámolom neked") {
        if (it.contentRaw == ".matek") {
            it.channel.sendMessage("Így használd a parancsot: `.matek <művelet>`\nPéldául: `.matek 2 + 2`")
                .queue { msg -> msg.makeRemovable() }
        } else {
            val jsMathMap = hashMapOf(
                "sin" to "Math.sin", "cos" to "Math.cos", "tan" to "Math.tan", "pi" to "Math.PI", "sqrt" to "Math.sqrt",
                "random" to "Math.random()", "rdm" to "Math.random()", "pow" to "Math.pow", "abs" to "Math.abs"
            )
            val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
            var input = it.contentRaw.removePrefix(".matek ")
            for (pair in jsMathMap) input = input.replace(pair.key, pair.value)
            val ans = engine.eval(input) as Number
            it.channel.sendMessage("$input = $ans").queue { msg -> msg.makeRemovable() }
        }
    }

    Command("brainfuck", ">++++++[<++++++++>-]<.") {
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

    Command("js", "tudok futtatni JavaScript kódot") {
        val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
        val input = it.contentRaw.removePrefix(".js").replace("```js", "")
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

    Command("vibe", "vibe-olunk együtt voice-ban?") {
        val vc = it.member?.voiceState?.channel
        if (it.contentRaw.contains("end")) {
            it.guild.audioManager.closeAudioConnection()
        } else if (it.contentRaw == ".vibe help") {
            it.channel.sendMessage("Vibe-oljunk!\n`.vibe (bababooey|bruh|in the 20s|otter|<YouTube link>)`")
                .queue { msg -> msg.makeRemovable() }
        } else {
            if (vc == null) {
                it.channel.sendMessage("Nem vagy hívásban, szóval nem tudom hová jöjjek.")
                    .queue { msg -> msg.makeRemovable() }
            } else {
                when (val param = it.contentRaw.removePrefix(".vibe").trim()) {
                    "bababooey" -> AudioModule.playSound(vc, AudioModule.SOUND_BABABOOEY)
                    "bruh" -> AudioModule.playSound(vc, AudioModule.SOUND_BRUH)
                    "in the 20s" -> AudioModule.playSound(vc, AudioModule.SOUND_VIBING20S)
                    "otter" -> AudioModule.playSound(vc, AudioModule.SOUND_OTTER)
                    else -> if (param.isNotEmpty()) AudioModule.playSound(vc, param) else AudioModule.joinVoice(vc)
                }
            }
        }
    }

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
            it.channel.sendMessage("$question\n🔃: volt már ❓: megoldás | Következő: `.emoji kvíz`").queue { msg ->
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
                                            "Következő: `.emoji kvíz`"
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
        if (it.contentRaw == ".hype") {
            it.channel.sendMessage("A parancs használata: `.hype <szám>`").queue { msg -> msg.makeRemovable() }
        } else {
            lateinit var listener: (e: MessageReactionAddEvent, m: Message) -> Unit
            val max = it.contentRaw.removePrefix(".hype ").toInt()
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
        """.*\b(baszdmeg|bazdmeg|fasz|gec|geci|kurva|ribanc|buzi|fuck|rohadj|picsa|picsába|rohadék|cigány).*""",
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
        if (it.contentRaw == ".akasztófa") {
            it.channel.sendMessage(
                EmbedBuilder().setTitle("Akasztófa")
                    .setDescription(
                        "Statisztikák: `.akasztófa stat`\n" +
                                "Új játék: `.akasztófa ||<szöveg>||` Pl: `.akasztófa ||gombóc||` vagy " +
                                "`.akasztófa ||játék||{❓}`\n" +
                                "Ezt a vonalat `|` így kell írni: `AltGr + W`\n" +
                                "Játék módosítók: ❓segítségek"
                    )
                    .build()
            ).queue { msg -> msg.makeRemovable() }
        } else if (it.contentRaw == ".akasztófa clear") {
            val isPlaying = data.hangmanGames.any { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
            if (isPlaying) {
                data.hangmanGames.removeIf { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
                it.channel.sendMessage("Akasztófa játék törölve :x:").queue { msg -> msg.makeRemovable() }
            } else it.channel.sendMessage("Itt nem volt akasztófa játék.").queue { msg -> msg.makeRemovable() }
        } else if (it.contentRaw.startsWith(".akasztófa stat")) {
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
            val param = it.contentRaw.removePrefix(".akasztófa ").replace("||", "")
                .replace("""\{.*}""".toRegex(), "")
            val mods = if (it.contentRaw.contains('{')) it.contentRaw.split('{')[1].removeSuffix("}") else ""
            it.channel.sendMessage(
                "**Akasztófa (${it.author.asTag})** `a.<betű>` Pl: `a.k` a kezdéshez\n```\n${
                    Hangman.toHangedText(
                        param,
                        ""
                    )
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
        if (hangGame == null) it.reply("Nem, nincs. Új játékhoz írd be, hogy `.akasztófa`").queue()
    }.setIsTrigger(true)

    Command("""a\.[a-záéíóöőúüű?]""", "akasztófa tipp") {
        val hangGame = data.hangmanGames.first { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
        hangGame.guess(it)
    }.setIsTrigger(true)
}

fun setNumGuesserGame() {
    Command("számkitaláló", "gondolok egy számra") {
        if (it.contentRaw == ".számkitaláló") {
            it.channel.sendMessage(
                EmbedBuilder()
                    .setTitle("Számkitaláló játékmódok:")
                    .setDescription(
                        "Alap: `.számkitaláló <max szám>` pl: `.számkitaláló 100`\n" +
                                "Hanna: `.számkitaláló hanna` (elég nagy kihívás)"
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