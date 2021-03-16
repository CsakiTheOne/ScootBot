package extra

import com.google.gson.Gson
import java.lang.Exception
import java.net.InetSocketAddress
import java.io.DataOutputStream
import java.io.DataInputStream
import java.net.Socket
import java.io.IOException
import java.nio.charset.Charset
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets

class Pinger {
    companion object {
        fun ping(address: String) : MinecraftServerInfo {
            try {
                val host = InetSocketAddress(address, 25565)
                val socket = Socket()
                socket.connect(host, 3000)
                val output = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())
                val handshakeMessage = createHandshakeMessage(address, 25565)
                // C->S : Handshake State=1
                // send packet length and packet
                writeVarInt(output, handshakeMessage!!.size)
                output.write(handshakeMessage)
                // C->S : Request
                output.writeByte(0x01) // size is only 1
                output.writeByte(0x00) // packet id for ping
                // S->C : Response
                val size = readVarInt(input)
                var packetId = readVarInt(input)
                if (packetId == -1) {
                    throw IOException("Premature end of stream.")
                }
                if (packetId != 0x00) { //we want a status response
                    throw IOException("Invalid packetID")
                }
                val length = readVarInt(input) //length of json string
                if (length == -1) {
                    throw IOException("Premature end of stream.")
                }
                if (length == 0) {
                    throw IOException("Invalid string length.")
                }
                val `in` = ByteArray(length)
                input.readFully(`in`) //read json string
                val json = String(`in`)
                // C->S : Ping
                val now = System.currentTimeMillis()
                output.writeByte(0x09) //size of packet
                output.writeByte(0x01) //0x01 for ping
                output.writeLong(now)
                readVarInt(input)
                packetId = readVarInt(input)
                if (packetId == -1) {
                    throw IOException("Premature end of stream.")
                }
                if (packetId != 0x01) {
                    throw IOException("Invalid packetID")
                }
                val pingtime = input.readLong()
                println(json)
                return Gson().fromJson(json, MinecraftServerInfo::class.java)
            }
            catch (ex: Exception) {
                return MinecraftServerInfo()
            }
        }

        @Throws(IOException::class)
        fun createHandshakeMessage(host: String, port: Int): ByteArray? {
            val buffer = ByteArrayOutputStream()
            val handshake = DataOutputStream(buffer)
            handshake.writeByte(0x00) //packet id for handshake
            writeVarInt(handshake, 4) //protocol version
            writeString(handshake, host, StandardCharsets.UTF_8)
            handshake.writeShort(port) //port
            writeVarInt(handshake, 1) //state (1 for handshake)
            return buffer.toByteArray()
        }

        @Throws(IOException::class)
        fun writeString(out: DataOutputStream, string: String, charset: Charset?) {
            val bytes = string.toByteArray(charset!!)
            writeVarInt(out, bytes.size)
            out.write(bytes)
        }

        @Throws(IOException::class)
        fun writeVarInt(out: DataOutputStream, paramInt: Int) {
            var paramInt = paramInt
            while (true) {
                if (paramInt and -0x80 == 0) {
                    out.writeByte(paramInt)
                    return
                }
                out.writeByte(paramInt and 0x7F or 0x80)
                paramInt = paramInt ushr 7
            }
        }

        @Throws(IOException::class)
        fun readVarInt(`in`: DataInputStream): Int {
            var i = 0
            var j = 0
            while (true) {
                val k = `in`.readByte().toInt()
                i = i or (k and 0x7F shl j++ * 7)
                if (j > 5) throw RuntimeException("VarInt too big")
                if (k and 0x80 != 128) break
            }
            return i
        }
    }
}