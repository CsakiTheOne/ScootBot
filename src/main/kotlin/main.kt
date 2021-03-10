import Bot.Companion.makeRemovable
import Bot.Companion.simplify
import com.google.gson.Gson
import extra.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.awt.Color
import java.io.File
import java.util.*
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.concurrent.timerTask

lateinit var data: Data
lateinit var bot: Bot
var autoActivity = true
var tags = mutableSetOf<String>()

var lastVibeCommand = "még senki sem kérte"
var hangmanGames = mutableListOf<Hangman>()
var numGuesserGames = mutableListOf<NumGuesser>()

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
    setHangmanGame()
    setNumGuesserGame()

    Timer().scheduleAtFixedRate(timerTask {
        if (!autoActivity) return@timerTask
        val activities = listOf(
            Activity.playing("${bot.getSelf().jda.guilds.size} szerveren"),
            Activity.playing("HealTogether"),
            Activity.playing("Minecraft"),
            Activity.playing("Terraria (köszönöm @Krey)"),
            Activity.playing("No Man's Sky"),
            Activity.playing("Random Bot"),
            Activity.playing("Sonic Mania"),
            Activity.listening(".help"),
            Activity.listening("Emerald Hill Zone"),
            Activity.listening("Fist Bump"),
            Activity.listening("Lifelight - AmaLee"),
            Activity.listening("Thunderstruck - AC/DC"),
            Activity.listening("TheFatRat"),
            Activity.listening("Lindsey Stirling"),
            Activity.watching("Disenchantment"),
            Activity.watching("TikTok: @csakivevo"),
            Activity.watching("Technoblade trolling Skeppy"),
            Activity.watching("Minecraft stream online órán"),
            Activity.watching("Unusual Memes"),
            Activity.watching("Mr. Robot"),
            Activity.watching("🟧⬛"),
        )
        bot.getSelf().jda.presence.activity = activities.random()
        Data.log("Activity manager", bot.getSelf().jda.presence.activity.toString())
    }, 3000L, 1000 * 60 * 5)
}

fun setHelp() {
    bot.commands.add(Command("help admin", "") {
        val helpMessage = "**Parancsok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> c.isAdminOnly }.joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true))

    bot.commands.add(Command("help játék", "nézd meg milyen játékokat játszhatsz") {
        val helpMessage = "**Játékok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> c.tags.contains(Command.TAG_GAME) }.joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc játékok")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    })

    bot.commands.add(Command("help", "ÁÁÁÁÁÁÁÁÁ!!!") {
        val helpMessage = "**Parancsok (mindegyik elé `${bot.prefix}` vagy szólítsd meg Gombócot):**\n" +
                bot.commands.filter { c -> !c.isAdminOnly }.joinToString("\n")
        it.channel.sendMessage(
            EmbedBuilder()
                .setColor(Color(0, 128, 255))
                .setTitle("Gombóc segítség")
                .setDescription(helpMessage)
                .build()
        ).queue { msg -> msg.makeRemovable() }
    })

    bot.commands.add(Command("custom", "saját parancsokat alkothatsz") {
        if (it.contentRaw == ".custom") {
            val helpMessage = "Új parancs: `.custom add <parancs>; [kimenet]; [egyéb beállítások]`\n" +
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
            if (data.customCommands.any { cc -> cc.command == params[0] }) {
                it.channel.sendMessage("Már van ilyen parancs!").queue { msg -> msg.makeRemovable() }
            }
            else {
                when (params.size) {
                    3 -> data.customCommands.add(CustomCommand(params[0], params[1], params[2].contains("❌")))
                    2 -> data.customCommands.add(CustomCommand(params[0], params[1], false))
                    1 -> data.customCommands.add(CustomCommand(params[0], "Ez a parancs nem csinál semmit.", true))
                }
                data.save()
                if (params.size < 4) it.channel.sendMessage("Új parancs hozzáadva ✅").queue { msg -> msg.makeRemovable() }
            }
        }
        else if (it.contentRaw.startsWith(".custom delete")) {
            data.customCommands.removeIf { cc -> cc.command == it.contentRaw.removePrefix(".custom delete ") }
            data.save()
            it.channel.sendMessage("Parancs törölve ✅").queue { msg -> msg.makeRemovable() }
        }
    })
}

fun setAdminCommands() {
    bot.commands.add(Command("status offline", "offline-ra állítja a botot") {
        bot.getSelf().jda.presence.setStatus(OnlineStatus.OFFLINE)
        println("Status set to ${bot.getSelf().jda.presence.status.name}")
    }.setIsAdminOnly(true))

    bot.commands.add(Command("activity", "bot állapot állítás") {
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
    }.setIsAdminOnly(true))

    bot.commands.add(Command("guilds", "szerverek mutatása") {
        val guilds = if (it.contentRaw == ".guilds") bot.getSelf().jda.guilds else bot.getSelf().jda.guilds.filter { g -> g.name.toLowerCase().contains(it.contentRaw.removePrefix(".guilds ")) }
        for (guild in guilds) {
            guild.loadMembers().onSuccess { members ->
                val bots = members.filter { m -> m.user.isBot }
                val humans = members.filter { m -> !m.user.isBot }
                val humansTextRaw = humans.joinToString { h -> h.user.asTag }
                val humansText = if (humansTextRaw.length < 1500) "\n$humansTextRaw" else ""
                it.channel.sendMessage(
                    "**${guild.name}** by ${guild.owner?.user?.asTag}\n```\nEmberek: ${humans.size} Botok: ${bots.size}$humansText\n```"
                ).queue { msg -> msg.makeRemovable() }
            }
        }
    }.setIsAdminOnly(true))

    bot.commands.add(Command("diary", "naplózó szoba beállítása") {
        data.diaryChannel = SimpleChannel(it.guild.id, it.channel.id)
        data.save()
        data.diary(bot.getSelf().jda, "Naplózás helye beállítva ✔") { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true))

    bot.commands.add(Command("log read", "részletes napló olvasása") {
        it.channel.sendMessage("```\n" + Data.logRead() + "\n```").queue { msg -> msg.makeRemovable() }
    }.setIsAdminOnly(true))

    bot.commands.add(Command("canstop", "biztonságos leállítás ellenőrzése") {
        val text = "${hangmanGames.size} akasztófa és ${numGuesserGames.size} számkitaláló játék van folyamatban. " +
                "Utolsó vibe parancs: $lastVibeCommand"
        it.channel.sendMessage(text).queue()
    }.setIsAdminOnly(true))

    bot.commands.add(Command("clear", "utolsó üzenetek törlése") {
        val count = it.contentRaw.removePrefix(".clear ").toInt() + 1
        bot.getSelf().jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)
        it.channel.history.retrievePast(count).queue { msgs ->
            for (i in 0 until msgs.size) {
                msgs[i].delete().queue {
                    println("[CLEAR]: Deleted message ${i}/${msgs.size - 1}")
                    if (i >= msgs.size - 1) {
                        bot.getSelf().jda.presence.setStatus(OnlineStatus.ONLINE)
                    }
                }
            }
        }
    }.setIsAdminOnly(true))
}

fun setBasicCommands() {
    bot.commands.add(Command("ping", "🏓") {
        it.channel.sendMessage(":ping_pong:").queue { msg -> msg.makeRemovable() }
    })

    bot.commands.add(Command("invite", "hívj meg a saját szerveredre") {
        it.channel.sendMessage("<https://discord.com/oauth2/authorize?client_id=783672257347715123&scope=bot&permissions=8>").queue { msg -> msg.makeRemovable() }
    })

    bot.commands.add(Command("tagok", "hány ember van ezen a szerveren?") {
        it.guild.loadMembers().onSuccess { members ->
            val csakiStatus = members.firstOrNull { m -> m.id == Data.admins[0].id }?.onlineStatus ?: OnlineStatus.UNKNOWN
            it.channel.sendMessage(
                EmbedBuilder()
                    .setTitle("Szerver tagok (${members.size})")
                    .setDescription(
                        "🟢 online: ${members.filter { m -> m.onlineStatus == OnlineStatus.ONLINE }.size}\n" +
                        "🟡 tétlen: ${members.filter { m -> m.onlineStatus == OnlineStatus.IDLE }.size}\n" +
                        "🔴 elfoglalt: ${members.filter { m -> m.onlineStatus == OnlineStatus.DO_NOT_DISTURB }.size}\n" +
                        "📡 offline: ${members.filter { m -> m.onlineStatus == OnlineStatus.OFFLINE }.size}\n" +
                        "🤖 bot: ${members.filter { m -> m.user.isBot }.size}\n" +
                        "Szerver tulaj (${it.guild.owner?.user?.name}): ${it.guild.owner?.onlineStatus}\n" +
                        "Gombóc tulaj (${Data.admins[0].name}): $csakiStatus"
                    )
                    .build()
            ).queue { msg -> msg.makeRemovable() }
        }
    })

    bot.commands.add(Command("mondd", "ki tudsz mondatni velem valamit") {
        if (Data.admins.any { admin -> admin.id == it.author.id }) {
            it.channel.sendMessage(it.contentRaw.removePrefix(".mondd ")).queue()
        }
        else {
            it.channel.sendMessage("*${it.contentRaw.removePrefix(".mondd ")}*").queue()
        }
    })

    bot.commands.add(Command("szavazás", "én egy demokratikusan megválasztott bot vagyok") {
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
    })

    bot.commands.add(Command("szegz", "nagyon romi") {
        val userFrom = it.author.asMention
        val userTo = it.contentRaw.split(' ')[1]
        if (it.textChannel.isNSFW) {
            it.channel.sendMessage("$userFrom megszegzeli őt: $userTo").queue { msg ->
                msg.addReaction(listOf("❤️", "\uD83D\uDE0F", "\uD83D\uDE1C", "\uD83D\uDE2E").random()).queue()
            }
        }
        else {
            it.channel.sendMessage("$userFrom, menj át egy nsfw szobába $userTo társaddal együtt.").queue()
        }
    })

    bot.commands.add(Command("gift", "küldj ajándékot a barátaidnak") {
        it.channel.sendMessage(
            EmbedBuilder()
                .setTitle("A wild gift appeared", "https://youtu.be/dQw4w9WgXcQ")
                .setThumbnail("https://static.wikia.nocookie.net/badcreepypasta/images/5/5e/Tumblr_966cc5d6fb3d9ef359cc10f994e26785_e24e7c68_640.png/revision/latest/scale-to-width-down/340?cb=20200229232309")
                .setDescription("```\n     Choccy Milk     \n```")
                .setColor(Color(199, 158, 120))
                .build()
        ).queue()
    })

    bot.commands.add(Command("matek", "írj be egy műveletet és kiszámolom neked") {
        if (it.contentRaw == ".matek") {
            it.channel.sendMessage("Így használd a parancsot: `.matek <művelet>`\nPéldául: `.matek 2 + 2`")
                .queue { msg -> msg.makeRemovable() }
        }
        else {
            val jsMathMap = hashMapOf(
                "sin" to "Math.sin", "cos" to "Math.cos", "tan" to "Math.tan", "pi" to "Math.PI", "sqrt" to "Math.sqrt",
                "random" to "Math.random()", "rdm" to "Math.random()", "pow" to "Math.pow", "abs" to "Math.abs"
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
    })

    bot.commands.add(Command("függvény", "tudok függvényt ábrázolni") {
        if (it.contentRaw == ".függvény") {
            it.channel.sendMessage("Így használd a parancsot: `.függvény <függvény teste>; <tartomány>` " +
                    "Például: `.függvény abs(x - 1) + 2; 5`\n").queue { msg -> msg.makeRemovable() }
        }
        else {
            val jsMathMap = hashMapOf(
                "sin" to "Math.sin", "cos" to "Math.cos", "tan" to "Math.tan", "pi" to "Math.PI", "sqrt" to "Math.sqrt",
                "random" to "Math.random()", "rdm" to "Math.random()", "pow" to "Math.pow", "abs" to "Math.abs"
            )
            val engine: ScriptEngine = ScriptEngineManager().getEngineByName("JavaScript")
            val inputRaw = it.contentRaw.removePrefix(".függvény ").split(';')[0].trim()
            var input = inputRaw
            val checkRange = it.contentRaw.split(';')[1].trim().toInt()
            for (pair in jsMathMap) {
                input = input.replace(pair.key, pair.value)
            }
            val f = mutableMapOf<Int, Float>()
            val v = mutableListOf<Float>()
            for (i in -checkRange..checkRange) {
                val x = (engine.eval(input.replace("x", i.toString())) as Number).toFloat()
                v.add(x)
                f[i] = x
            }
            var graph = ""
            for (y in -checkRange..checkRange) {
                for (x in -checkRange..checkRange) {
                    graph += if (f[x]?.toInt() ?: 0 == -y) "██"
                    else if (y == 0) "--"
                    else if (x == 0) " |"
                    else "  "
                }
                graph += "\n"
            }
            val ans = f.toString()
            it.channel.sendMessage("f(x) = $inputRaw\n```\n$ans\n$graph\n```").queue { msg -> msg.makeRemovable() }
        }
    })

    bot.commands.add(Command("brainfuck", ">++++++[<++++++++>-]<.") {
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
    })

    bot.commands.add(Command("js", "tudok futtatni JavaScript kódot") {
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
    })

    bot.commands.add(Command("vibe", "vibe-olunk együtt voice-ban?") {
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
                lastVibeCommand = Calendar.getInstance().time.toString()
            }
        }
    })

    bot.commands.add(Command("emoji kvíz", "találd ki, hogy mit jelentenek az emoji-k") {
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
            it.channel.sendMessage("$question\n🔃: volt már ❓: megoldás | Következő: `.emoji kvíz`").queue { msg ->
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
    }.addTag(Command.TAG_GAME))

    bot.commands.add(Command("repost", "vót má'") {
        it.channel.sendFile(File("./repost.jpg")).queue { msg -> msg.makeRemovable() }
    })

    bot.commands.add(Command("hype", "ébreszd fel a szervert reakció gyűjtős játékkal") {
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
                        rmsg.editMessage("**Hype!** $n/$max Reagálj erre az üzenetre! 🎉\n`8${"=".repeat(percent * 20 / 100)}${" ".repeat(20 - (percent * 20 / 100))}D` $percentNoLimit%").queue()
                    }
                    else {
                        rmsg.editMessage("**Hype!** $n/$max\n$percentNoLimit% 🎉").queue()
                    }
                }
            }
            it.channel.sendMessage("**Hype!** Reagálj erre az üzenetre! 🎉\n`[       START!       ]` ${max * 3} másodpercetek van!").queue { msg ->
                listener = bot.addReactionListener { event -> onHypeReact(event, msg) }
            }
            Timer().schedule(timerTask {
                bot.reactionListeners.remove(listener)
                it.channel.sendMessage("Hype vége! 🎉 ||Kell egy kis idő a reakciók összeszámolásához, de szép volt!||").queue()
            }, (max * 3 * 1000).toLong())
        }
    }.addTag(Command.TAG_GAME))

    bot.commands.add(Command("fejlesztő", "ha érdekel ki alkotott") {
        it.channel.sendMessage("A készítőm: **@CsakiTheOne#8589** De sokan segítettek jobbá válni ❤").queue()
    })
}

fun setBasicTriggers() {
    bot.triggers[".*@random.*"] = {
        it.guild.loadMembers().onSuccess { members ->
            val randomMember = members.filter { m ->
                m.onlineStatus != OnlineStatus.OFFLINE && m.onlineStatus != OnlineStatus.DO_NOT_DISTURB &&
                !m.user.isBot && m.user != it.author
            }.random()
            it.reply(randomMember.asMention).queue()
        }
    }

    bot.triggers["""((szia|helló|hali|csá|cső|hey|henlo) gombóc!*)|sziaszto+k.*|gombóc."""] = {
        val greetings = listOf("Szia!", "Hali!", "Henlo!", "Hey!", "Heyho!")
        it.channel.sendMessage(greetings.random()).queue()
    }

    bot.triggers["""mit csináltok?.*|ki mit csinál?.*|mit csinálsz gombóc?.*"""] = {
        val activityType = when (bot.getSelf().jda.presence.activity?.type) {
            Activity.ActivityType.LISTENING -> "Zenét hallgatok 🎧"
            Activity.ActivityType.WATCHING -> "Videót nézek 📽"
            else -> "Játszok 🎮"
        }
        it.channel.sendMessage(activityType).queue()
    }

    bot.triggers["kő|papír|olló|\uD83E\uDEA8|\uD83E\uDDFB|✂️"] = {
        val answers = listOf("Kő \uD83E\uDEA8", "Papír \uD83E\uDDFB", "Olló ✂️")
        it.reply(answers.random()).queue { msg -> msg.makeRemovable() }
    }

    bot.triggers[".*szeret.*"] = {
        if (!it.contentRaw.simplify().contains("nem szeret")) {
            it.addReaction("❤️").queue()
        }
    }

    bot.triggers["""jó {0,1}éj.*"""] = {
        val greetings = listOf("Aludj jól!", "Álmodj szépeket!", "Jó éjt!", "Jó éjszakát!", "Pihend ki magad!", "Kitartást holnapra!")
        if (!tags.contains("cooldown_goodnight")) {
            it.channel.sendMessage(greetings.random()).queue()
            tags.add("cooldown_goodnight")
            Timer().schedule(timerTask {
                tags.remove("cooldown_goodnight")
            }, 20000)
        }
    }

    bot.triggers[""".*\b(baszdmeg|bazdmeg|fasz|gec|geci|kurva|ribanc|buzi|fuck|rohadj|picsa|picsába|rohadék).*"""] = {
        if (!it.channel.name.simplify().contains("nsfw")) {
            it.addReaction("😠").queue()
            data.diary(bot.getSelf().jda, "${it.author.asTag} csúnyán beszélt a(z) ${it.channel.name} szobában. Azt, mondta hogy:\n${it.contentRaw}")
        }
    }

    bot.triggers[".*(kapitalizmus|kapitalista).*"] = {
        data.diary(bot.getSelf().jda, "${it.author.asTag} a(z) ${it.channel.name} szobában a ||kapitalizmust|| emlegette.")
    }

    bot.triggers[""".*\b(csáki).*"""] = {
        val guildName = if (it.isFromGuild) "**Szerver:** ${it.guild.name} > ${it.channel.name}" else "**Privát:** ${it.channel.name}"
        val embed = EmbedBuilder()
            .setTitle("Említés egy üzenetben (Nem biztos, hogy PONT rólad van szó, csak azt figyelem hogy benne van-e egy bizonyos szöveg az üzenetben)")
            .setDescription("$guildName\n**Üzenet:** ${it.contentRaw}\n**Írta:** ${it.author.asTag}")
            .build()
        bot.getSelf().jda.getPrivateChannelById(Data.admins[0].privateChannel)?.sendMessage(embed)?.queue()
    }
}

fun setClickerGame() {
    bot.commands.add(Command("clicker", "mint a cookie clicker") {
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
    }.addTag(Command.TAG_GAME))

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
            data.clicks["global"] = (data.clicks["global"] ?: 0) + 1
            data.save()
            val clicks = (msg.embeds[0].description!!.split("\n")[1].split(":")[1].trim().toInt()) + 1
            msg.editMessage(
                EmbedBuilder()
                    .setColor(Color((0..255).random(), (0..255).random(), (0..255).random()))
                    .setTitle("Clicker játék")
                    .setDescription("🌍: ${data.clicks["global"] ?: 0}\n🖱: $clicks")
                    .build()
            ).queue { _ ->
                it.reaction.removeReaction(it.user!!).queue()
            }
        }
    }
}

fun setHangmanGame() {
    bot.commands.add(Command("akasztófa", "G--bóc") {
        if (it.contentRaw == ".akasztófa") {
            it.channel.sendMessage("Parancs használat: `.akasztófa ||<szöveg>||` Például: `.akasztófa ||gombóc||`").queue { msg -> msg.makeRemovable() }
        }
        else {
            val param = it.contentRaw.removePrefix(".akasztófa ").replace("||", "")
            it.channel.sendMessage("**Akasztófa (${it.author.asTag})** Tipphez: `a.<betű>` Például: `a.k`\n```\n${Hangman.toHangedText(param, "")}\n```").queue { msg ->
                val newGame = Hangman(it.author.asTag, it.guild.id, it.channel.id, msg.id, param, "")
                hangmanGames.add(newGame)
                if (!newGame.toHangedText().contains("-")) {
                    msg.makeRemovable()
                    hangmanGames.remove(newGame)
                }
            }
        }
    }.addTag(Command.TAG_GAME))

    bot.triggers["""a\.[a-z]"""] = {
        val c = it.contentRaw.toLowerCase()[2]
        val hangGame = hangmanGames.first { h -> h.guildId == it.guild.id && h.channelId == it.channel.id }
        hangGame.players.add(it.author.asMention)
        hangGame.chars += c
        hangGame.sendMessage(bot.getSelf().jda, data)
        if (hangGame.getIsGameEnded() != 0) {
            hangmanGames.remove(hangGame)
        }
        it.delete().queue()
    }
}

fun setNumGuesserGame() {
    bot.commands.add(Command("számkitaláló", "gondolok egy számra") {
        if (it.contentRaw == ".számkitaláló") {
            it.channel.sendMessage(
                EmbedBuilder()
                    .setTitle("Számkitaláló játékmódok:")
                    .setDescription(
                        "Alap: `.számkitaláló <max szám>` pl: `.számkitaláló 100`\n" +
                        "Betű: `.számkitaláló abc`\n" +
                        "Hanna: `.számkitaláló hanna` (elég nagy kihívás)"
                    )
                    .build()
            ).queue { msg -> msg.makeRemovable() }
        }
        else {
            val param = it.contentRaw.split(" ")[1]
            var introText = ""
            when (param) {
                "abc" -> {
                    introText = "Gondoltam egy betűre az (angol) ABC-ből. Tippeléshez írj egy betűt!"
                    it.channel.sendMessage(introText).queue { msg ->
                        numGuesserGames.add(NumGuesser(it.author.asTag, it.guild.id, it.channel.id, msg.id, (('a'.toInt())..('z'.toInt())).random(), mutableListOf("char")))
                    }
                }
                "hanna" -> {
                    introText = "Gondoltam egy számra **${Int.MIN_VALUE} és ${Int.MAX_VALUE}** között. Tipphez írd le simán a számot chat-re!"
                    it.channel.sendMessage(introText).queue { msg ->
                        numGuesserGames.add(NumGuesser(it.author.asTag, it.guild.id, it.channel.id, msg.id, (Int.MIN_VALUE..Int.MAX_VALUE).random(), mutableListOf("hanna")))
                    }
                }
                else -> {
                    val max = (0..(param.toInt())).random()
                    introText = "Gondoltam egy számra **0 és $max** között. Tipphez írd le simán a számot chat-re!"
                    it.channel.sendMessage(introText).queue { msg ->
                        numGuesserGames.add(NumGuesser(it.author.asTag, it.guild.id, it.channel.id, msg.id, (0..max).random(), mutableListOf()))
                    }
                }
            }
        }
    }.addTag(Command.TAG_GAME))

    bot.triggers["[a-z]"] = {
        val c = it.contentRaw.toLowerCase()[0]
        val x = c.toInt()
        val numGuesser = numGuesserGames.first { ng -> ng.guildId == it.guild.id && ng.channelId == it.channel.id && ng.tags.contains("char") }
        when {
            x > numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n`${it.author.name}: ? < $c`").queue()
                }
            }
            x < numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n`${it.author.name}: ? > $c`").queue()
                }
            }
            x == numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name} eltalálta, hogy a betű $c! 🎉\nÚj játék: `.számkitaláló`").queue {
                        msg.makeRemovable()
                        numGuesserGames.remove(numGuesser)
                    }
                }
            }
        }
        it.delete().queue()
    }

    bot.triggers["-{0,1}[0-9]+"] = {
        val x = it.contentRaw.toInt()
        val numGuesser = numGuesserGames.first { ng -> ng.guildId == it.guild.id && ng.channelId == it.channel.id }
        when {
            x > numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    var newContent = "${msg.contentRaw}\n`${it.author.name}: X < $x`"
                    if (newContent.length > 1000) newContent += " (kevesebb, mint ${2000 - newContent.length} karakter maradt)"
                    it.channel.editMessageById(numGuesser.messageId, newContent).queue()
                }
            }
            x < numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    var newContent = "${msg.contentRaw}\n`${it.author.name}: X > $x`"
                    if (newContent.length > 1000) newContent += " (kevesebb, mint ${2000 - newContent.length} karakter maradt)"
                    it.channel.editMessageById(numGuesser.messageId, newContent).queue()
                }
            }
            x == numGuesser.num -> {
                it.channel.retrieveMessageById(numGuesser.messageId).queue { msg ->
                    if (it.author.name == "pussyhunter") {
                        it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name}: X `😝` $x`").queue()
                    }
                    else {
                        it.channel.editMessageById(numGuesser.messageId, "${msg.contentRaw}\n${it.author.name} eltalálta, hogy a szám $x! 🎉\nÚj játék: `.számkitaláló`").queue { edited ->
                            msg.makeRemovable()
                            if (numGuesser.tags.contains("hanna")) {
                                data.diary(bot.getSelf().jda, "${it.author.asTag} kitalálta a számot a legnehezebb szinten.")
                            }
                            numGuesserGames.remove(numGuesser)
                        }
                    }
                }
            }
        }
        it.delete().queue()
    }
}