package extra

class Hangman(
    val guildId: String,
    val channelId: String,
    val messageId: String,
    val text: String,
    var chars: String,
) {
    fun toHangedText() : String {
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
            val unmaskedChars = " .,:;?!()0123456789"
            var newText = ""
            for (c in text) {
                newText += if (chars.contains(c) || unmaskedChars.contains(c)) c else '-'
            }
            return newText
        }

        val graphcs = listOf(
            "\n\n\n\n\n",
            "\n\n\n\n\n|___",
            "\n|\n|\n|\n|\n|___",
            "____\n|\n|\n|\n|\n|___",
            "____\n" +
                    "|  |\n" +
                    "|\n" +
                    "|\n" +
                    "|\n" +
                    "|___",
            "____\n" +
                    "|  |\n" +
                    "|  o\n" +
                    "|\n" +
                    "|\n" +
                    "|___",
            "____\n" +
                    "|  |\n" +
                    "|  o\n" +
                    "| /|\n" +
                    "|\n" +
                    "|___",
            "____\n" +
                    "|  |\n" +
                    "|  o\n" +
                    "| /|\\\n" +
                    "|\n" +
                    "|___",
            "____\n" +
                    "|  |\n" +
                    "|  o\n" +
                    "| /|\\\n" +
                    "| /\n" +
                    "|___",
            "____\n" +
                    "|  |\n" +
                    "|  o\n" +
                    "| /|\\\n" +
                    "| / \\\n" +
                    "|___"
        )
    }
}