package extra

import Bot.Companion.makeRemovable
import Data
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel

class Hangman(
    val authorId: String,
    val guildId: String,
    val channelId: String,
    val text: String,
    val modifiers: String,
    var messageId: String,
    var chars: String,
) {
    var players = mutableSetOf<String>()

    fun getIsGameEnded() : Int {
        return if (toHangedText(true) == text) 1
        else if (getWrongChars().size >= graphcs.size - 1) 2
        else 0
    }

    fun guess(jda: JDA, data: Data, msg: Message) {
        if ("""a\.[a-záéíóöőúüű]""".toRegex().matches(msg.contentRaw)) {
            val c = msg.contentRaw.toLowerCase()[2]
            if (!chars.contains(c)) chars += c
        }
        else if (msg.contentRaw == "a.?" && modifiers.contains("❓")) {
            val randomChar = text.filter { c -> !toHangedText().contains(c) }.random()
            chars += randomChar
            chars += "❓"
            chars += "❓"
        }
        players.add(msg.author.id)
        sendMessage(jda, data, msg.channel)
        if (getIsGameEnded() != 0) {
            data.addHangmanStat(PlayerStats(authorId, words = mutableListOf(text)))
            for (player in players) {
                data.addHangmanStat(PlayerStats(player, 1, if (getIsGameEnded() == 1) 1 else 0))
            }
            if (getIsGameEnded() == 2) {
                data.addHangmanStat(PlayerStats(authorId, hangs = 1))
            }
            data.hangmanGames.remove(this)
        }
        msg.delete().queue()
        data.save()
    }

    fun sendMessage(jda: JDA, data: Data, channel: MessageChannel) {
        val author = jda.getUserById(authorId)
        val playerNames = mutableListOf<String>()
        for (player in players) {
            playerNames.add(jda.getUserById(player)!!.asTag)
        }
        fun sendNext() {
            val textPartHelp = if (getIsGameEnded() != 0) "" else " Tipp: `a.<betű>` Pl: `a.k` Modok: $modifiers"
            var modHelp = ""
            if (getIsGameEnded() == 0 && modifiers.contains("❓")) modHelp += "Segítség: a.? -2 hibalehetőség 1 betűért cserébe. "
            val g = if (getWrongChars().size >= graphcs.size) graphcs.last() else graphcs[getWrongChars().size]
            val textPartPlayers = if (getIsGameEnded() != 0) "\nJátékosok: ${playerNames.joinToString()}" else ""
            val messageText = "**Akasztófa (${author?.asTag})**$textPartHelp\n```\n" +
                    "$modHelp$g\n${toHangedText()}\n❤${graphcs.size - getWrongChars().size - 1} ${getWrongChars()}\n" +
                    "```$textPartPlayers"
            channel.sendMessage(messageText).queue { msg ->
                messageId = msg.id
                if (getIsGameEnded() != 0) msg.makeRemovable()
            }
            if (getIsGameEnded() == 2) {
                data.diary(jda, "${author?.asTag}```\n${graphcs.last()}\n$text\n```$textPartPlayers")
            }
        }
        channel.retrieveMessageById(messageId).queue({
            channel.deleteMessageById(messageId).queue {
                sendNext()
            }
        }, {
            sendNext()
        })
    }

    fun toHangedText(forceFormat: Boolean = false) : String {
        if (!forceFormat && getIsGameEnded() != 0) return text
        return Companion.toHangedText(text, chars)
    }

    fun getWrongChars() : List<Char> {
        val wrong = mutableListOf<Char>()
        for (c in chars) {
            if (!text.toLowerCase().contains(c) || c == '❓') wrong.add(c)
        }
        return wrong
    }

    companion object {
        fun toHangedText(text: String, chars: String) : String {
            var maskOverride = false
            var newText = ""
            for (c in text) {
                if (c == '*') maskOverride = !maskOverride
                else newText += if (chars.contains(c) || chars.toUpperCase().contains(c) || !c.toString().matches("[a-záéíóöőúüűA-ZÁÉÍÓÖŐÚÜŰ]".toRegex()) || maskOverride) c else '-'
            }
            return newText
        }

        val graphcs = listOf(
            "\n\n\n\n\n",
            "\n\n\n\n\nI___",
            "\n|\n|\n|\n|\nI___",
            "____\n|\n|\n|\n|\nI___",
            "____\n" +
                    "|  |\n" +
                    "|\n" +
                    "|\n" +
                    "|\n" +
                    "I___",
            "____\n" +
                    "|  |\n" +
                    "|  🤕\n" +
                    "|\n" +
                    "|\n" +
                    "I___",
            "____\n" +
                    "|  |\n" +
                    "|  🤨\n" +
                    "|  |\n" +
                    "|\n" +
                    "I___",
            "____\n" +
                    "|  |\n" +
                    "|  😮\n" +
                    "| /|\n" +
                    "|\n" +
                    "I___",
            "____\n" +
                    "|  |\n" +
                    "|  😃\n" +
                    "| /|\\\n" +
                    "|\n" +
                    "I___",
            "____\n" +
                    "|  |\n" +
                    "|  😳\n" +
                    "| /|\\\n" +
                    "| /\n" +
                    "I___",
            "____ Utolsó esély\n" +
                    "|  |\n" +
                    "|  😫\n" +
                    "| /|\\\n" +
                    "| / \\\n" +
                    "I___",
            "____ R.I.P.\n" +
                    "|  |\n" +
                    "|  💀\n" +
                    "| /|\\\n" +
                    "| / \\\n" +
                    "I___"
        )
    }

    class PlayerStats(
        val playerId: String,
        var games: Int = 0,
        var wins: Int = 0,
        var words: MutableList<String> = mutableListOf(),
        var hangs: Int = 0,
    ) {
        fun add(stat: PlayerStats) {
            games += stat.games
            wins += stat.wins
            words.addAll(stat.words)
            hangs += stat.hangs
        }

        override fun toString(): String {
            var text = "🎮$wins/$games, 📕${words.size}, 💀$hangs"
            if (words.isNotEmpty()) text += ", ${words.random()}"
            return text
        }
    }
}