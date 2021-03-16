package extra

import Bot.Companion.makeRemovable
import Data
import net.dv8tion.jda.api.JDA

class Hangman(
    val authorId: String,
    val guildId: String,
    val channelId: String,
    var messageId: String,
    val text: String,
    var chars: String,
) {
    var players = mutableSetOf<String>()

    fun getIsGameEnded() : Int {
        return if (toHangedText(true) == text) 1
        else if (getWrongChars().size >= graphcs.size - 1) 2
        else 0
    }

    fun sendMessage(jda: JDA, data: Data) {
        val author = jda.getUserById(authorId)
        val playerNames = mutableListOf<String>()
        for (player in players) {
            playerNames.add(jda.getUserById(player)!!.asTag)
        }
        val channel = jda.getTextChannelById(channelId)!!
        channel.deleteMessageById(messageId).queue()
        val textPartHelp = if (getIsGameEnded() != 0) "" else " Tipphez: `a.<betű>` Pl: `a.k`"
        val textPartPlayers = if (getIsGameEnded() != 0) "\nJátékosok: ${playerNames.joinToString()}" else ""
        val messageText = "**Akasztófa (${author?.asTag})**$textPartHelp\n```\n" +
                "${graphcs[getWrongChars().size]}\n${toHangedText()}\n${getWrongChars()}\n```$textPartPlayers"
        channel.sendMessage(messageText).queue { msg ->
            messageId = msg.id
            if (getIsGameEnded() != 0) {
                msg.makeRemovable()
            }
        }
        if (getIsGameEnded() == 2) {
            data.diary(jda, "${author?.asTag}```\n${graphcs.last()}\n$text\n```$textPartPlayers")
        }
    }

    fun toHangedText(forceFormat: Boolean = false) : String {
        if (!forceFormat && getIsGameEnded() != 0) return text
        return Companion.toHangedText(text, chars)
    }

    fun getWrongChars() : List<Char> {
        val wrong = mutableListOf<Char>()
        for (c in chars) {
            if (!text.contains(c)) {
                wrong.add(c)
            }
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
            if (words.isNotEmpty()) text += ", 🎲${words.random()}"
            return text
        }
    }
}