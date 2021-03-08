package extra

import Bot.Companion.makeRemovable
import Data
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageChannel

class Hangman(
    val authorTag: String,
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
        val channel = jda.getTextChannelById(channelId)!!
        channel.deleteMessageById(messageId).queue()
        val textPartHelp = if (getIsGameEnded() != 0) "" else " Tipphez: `a.<betű>` Pl: `a.k`"
        val textPartPlayers = if (getIsGameEnded() != 0) "\nJátékosok: ${players.joinToString()}" else ""
        val messageText = "**Akasztófa ($authorTag)**$textPartHelp\n```\n" +
                "${graphcs[getWrongChars().size]}\n${toHangedText()}\n${getWrongChars()}\n```$textPartPlayers"
        channel.sendMessage(messageText).queue { msg ->
            messageId = msg.id
            if (getIsGameEnded() != 0) {
                msg.makeRemovable()
            }
        }
        if (getIsGameEnded() == 2) {
            data.diary(jda, "```\n${graphcs.last()}\n$text\n```\n$textPartPlayers")
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
            "____\n" +
                    "|  |\n" +
                    "|  💀\n" +
                    "| /|\\\n" +
                    "| / \\\n" +
                    "I___"
        )
    }
}