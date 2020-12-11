package extra.adventure

import Data
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

class Adventure {
    companion object {
        private val mapW = 7
        private val mapH = 5
        private val digable = ":white_small_square:"
        private val loot = mutableListOf(
            "💩", "🦴", "🎈", "🧨", "🎞", "🎨", "🧶", "🎩", "🎓", "💍", "💎", "⚽", "🏀", "🎱", "🎮", "🎲", "🪀", "♟", "🔑",
            "🧱", "⚙", "🛡", "🏹", "🗡", "🩹", "💊", "💣", "📕", "✏", "📌", "🍕", "🍔", "🍟", "🥚", "🍭", "🍑", "🍒", "🍄",
            "🍆"
        )
        private lateinit var map: MutableList<MutableList<String>>
        private var playerX = 0
        private var playerY = 0
        private var inventory = mutableListOf<String>()

        init {
            reload()
        }

        private fun reload() {
            var c = 'a'
            while (c <= 'z') {
                loot.add(":regional_indicator_$c:")
                c++
            }

            map = mutableListOf()
            (0..mapW).forEach { _ ->
                val innerList = mutableListOf<String>()
                (0..mapH).forEach {
                    innerList.add(
                        if ((0..10).random() < 6)
                            if ((0..10).random() < 4) loot.random()
                            else digable
                        else ""
                    )
                }
                map.add(innerList)
            }
            playerX = (1..mapW).random() - 1
            playerY = (1..mapH).random() - 1
        }

        private fun getDescription() : String {
            var mapText = ""
            for (y in 0 until mapH) {
                for (x in 0 until mapW) {
                    mapText += if (playerX == x && playerY == y) {
                        ":llama:"
                    }
                    else if (map[x][y].isEmpty()) {
                        ":black_large_square:"
                    }
                    else {
                        map[x][y]
                    }
                }
                mapText += "\n"
            }
            val hint = "⛏: fölszedés / 1. tárgy lerakása\n👜: táska megrázása"
            var output = "$mapText\n$hint\n$inventory"
            if (inventory.isNotEmpty()) output += "\nKiválasztott tárgy: ${inventory[0]}"
            return output
        }

        private fun refreh(msg: Message) {
            msg.editMessage(
                EmbedBuilder()
                    .setTitle("Gombóc kaland")
                    .setDescription(getDescription())
                    .build()
            ).queue { adventureMessage ->
                adventureMessage.addReaction("⬅").queue()
                adventureMessage.addReaction("⬆").queue()
                adventureMessage.addReaction("⬇").queue()
                adventureMessage.addReaction("➡").queue()
                adventureMessage.addReaction("⛏").queue()
                adventureMessage.addReaction("👜").queue()
                adventureMessage.addReaction("🔄").queue()
                adventureMessage.addReaction("❌").queue()
            }
        }

        private fun btnA() {
            if (map[playerX][playerY] == digable) {
                map[playerX][playerY] = ""
                inventory.add(loot.random())
            }
            else if (map[playerX][playerY].isNotEmpty()) {
                val newItem = map[playerX][playerY]
                map[playerX][playerY] = ""
                inventory.add(newItem)
            }
            else if (inventory.isNotEmpty() || map[playerX][playerY] != digable) {
                val newItem = inventory[0]
                map[playerX][playerY] = newItem
                inventory.removeAt(0)
            }
        }

        private fun btnB() {
            val tempItem = inventory[0]
            inventory.removeAt(0)
            inventory.add(tempItem)
        }

        fun startNew(data: Data, msg: Message) {
            msg.channel.sendMessage(
                EmbedBuilder()
                    .setTitle("Gombóc kaland")
                    .setDescription(getDescription())
                    .build()
            ).queue { adventureMessage ->
                adventureMessage.addReaction("⬅").queue()
                adventureMessage.addReaction("⬆").queue()
                adventureMessage.addReaction("⬇").queue()
                adventureMessage.addReaction("➡").queue()
                adventureMessage.addReaction("⛏").queue()
                adventureMessage.addReaction("👜").queue()
                adventureMessage.addReaction("🔄").queue()
                adventureMessage.addReaction("❌").queue()
            }
        }

        fun buttonPressed(data: Data, event: MessageReactionAddEvent, msg: Message) {
            when (event.reactionEmote.name) {
                "⬅" -> if (playerX > 0) playerX--
                "⬆" -> if (playerY > 0) playerY--
                "⬇" -> if (playerY < mapH - 1) playerY++
                "➡" -> if (playerX < mapW - 1) playerX++
                "⛏" -> btnA()
                "👜" -> btnB()
                "🔄" -> reload()
                "❌" -> msg.delete().queue()
            }
            if (event.reactionEmote.name == "🔄" || event.reactionEmote.name == "❌") return
            msg.removeReaction(event.reactionEmote.name, event.user!!).queue {
                refreh(msg)
            }
        }
    }
}