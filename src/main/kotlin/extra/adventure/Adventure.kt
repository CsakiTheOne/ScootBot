package extra.adventure

import Data
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

class Adventure {
    companion object {
        private val mapW = 100
        private val mapH = 100
        private var map = mutableListOf(mutableListOf<String>())
        private var playerX = 0
        private var playerY = 0

        init {
            (0..mapW).forEach {
                val innerList = mutableListOf<String>()
                (0..mapH).forEach { _ -> innerList.add("") }
                map.add(innerList)
            }
            playerX = mapW / 2
            playerY = mapH / 2
        }

        private fun getDescription() : String {
            return "X: $playerX Y: $playerY\n${map[playerX][playerY]}"
        }

        private fun refreh(data: Data) {
            data.adventureMessages.forEach {
                it.editMessage(
                    EmbedBuilder()
                        .setTitle("Gombóc kaland")
                        .setDescription(getDescription())
                        .build()
                ).queue { adventureMessage ->
                    adventureMessage.addReaction("⬅").queue()
                    adventureMessage.addReaction("⬆").queue()
                    adventureMessage.addReaction("⬇").queue()
                    adventureMessage.addReaction("➡").queue()
                    adventureMessage.addReaction("🅰").queue()
                    adventureMessage.addReaction("🅱").queue()
                    adventureMessage.addReaction("❌").queue()
                }
            }
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
                adventureMessage.addReaction("🅰").queue()
                adventureMessage.addReaction("🅱").queue()
                adventureMessage.addReaction("❌").queue()
                data.adventureMessages.add(adventureMessage)
                data.save()
            }
        }

        fun buttonPressed(data: Data, event: MessageReactionAddEvent, msg: Message) {
            when (event.reactionEmote.name) {
                "⬅" -> {
                    playerX--
                }
                "⬆" -> {
                    playerY--
                }
                "⬇" -> {
                    playerY++
                }
                "➡" -> {
                    playerX++
                }
                "🅰" -> {
                    map[playerX][playerY] = ":hole:"
                }
                "🅱" -> {
                    map[playerX][playerY] = ""
                }
                "❌" -> {
                    data.adventureMessages.remove(msg)
                    data.save()
                    msg.delete().queue()
                }
            }
            msg.removeReaction(event.reactionEmote.name, event.user!!).queue {
                refreh(data)
            }
        }
    }
}