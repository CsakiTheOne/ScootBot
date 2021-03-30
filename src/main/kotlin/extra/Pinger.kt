package extra

import Global.Companion.jda
import com.google.gson.Gson
import com.google.gson.JsonParser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import java.awt.Color
import java.lang.Exception
import java.net.InetSocketAddress
import java.io.DataOutputStream
import java.io.DataInputStream
import java.net.Socket
import java.io.IOException
import java.nio.charset.Charset
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

class Pinger {
    companion object {
        fun pingMinecraftServer(port: Int) {
            val mcStat = pingMcsrvstat(port)
            val json = JsonParser.parseString(mcStat).asJsonObject

            val isOnline = json.getAsJsonPrimitive("online").asBoolean
            val motd = if (!isOnline) "offline" else json.getAsJsonObject("motd").getAsJsonArray("clean").asString
            val playerCount = if (!isOnline) 0 else json.getAsJsonObject("players")?.getAsJsonPrimitive("online") ?: 0
            val playerMax = if (!isOnline) 0 else json.getAsJsonObject("players")?.getAsJsonPrimitive("max") ?: 0
            val players = if (!isOnline) listOf() else json.getAsJsonObject("players")?.getAsJsonArray("list")?.toList()
                ?: listOf()
            val version = if (!isOnline) "" else json.getAsJsonPrimitive("version").asString

            val time = "${Calendar.getInstance()[Calendar.HOUR_OF_DAY]}-${Calendar.getInstance()[Calendar.MINUTE]}"
            val channel = jda.getGuildChannelById("821985457218781204") as TextChannel?

            channel?.manager?.setName(
                if (isOnline) "mc🟢${players.size}-🕒$time" else "mc💤-🕒$time"
            )?.queue()

            var text = "```\n$motd\n```\n"
            if (isOnline) text = "**$version**\n$text"
            text += if (players.isEmpty()) "Nincs fönt senki a szerveren."
            else "**Játékosok ($playerCount/$playerMax)**\n${players.joinToString()}"
            channel?.editMessageById(
                "821989475436724275",
                EmbedBuilder()
                    .setTitle("Minecraft szerver infó")
                    .setDescription(text)
                    .setFooter("Utolsó ping: ${time.replace('-', ':')}")
                    .setColor(if (isOnline) Color.GREEN else Color.RED)
                    .build()
            )?.queue()
        }

        fun pingMcsrvstat(port: Int): String {
            val ip = getPublicIP()
            return URL("https://api.mcsrvstat.us/2/$ip:$port").readText()
        }

        fun getPublicIP(): String {
            return URL("https://api.ipify.org").readText()
        }
    }
}