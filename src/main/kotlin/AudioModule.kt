import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import extra.AudioPlayerSendHandler
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.VoiceChannel

class AudioModule {
    companion object {
        private var audioPlayerManagers = mutableMapOf<String, AudioPlayerManager>()
        private var audioPlayers = mutableMapOf<String, AudioPlayer>()

        private fun createPlayer(guildId: String): Boolean {
            if (audioPlayerManagers[guildId] == null) {
                audioPlayerManagers[guildId] = DefaultAudioPlayerManager()
                audioPlayerManagers[guildId]!!.registerSourceManager(LocalAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY))
                audioPlayerManagers[guildId]!!.registerSourceManager(YoutubeAudioSourceManager(true))
                audioPlayers[guildId] = audioPlayerManagers[guildId]!!.createPlayer()
                audioPlayers[guildId]!!.volume = 20
                return true
            }
            return false
        }

        fun joinVoice(vc: VoiceChannel) {
            createPlayer(vc.guild.id)
            val handler: AudioSendHandler = AudioPlayerSendHandler(audioPlayers[vc.guild.id]!!)
            vc.guild.audioManager.sendingHandler = handler
            vc.guild.audioManager.openAudioConnection(vc)
        }

        fun playSound(vc: VoiceChannel, sound: String) {
            joinVoice(vc)
            audioPlayerManagers[vc.guild.id]!!.loadItem(sound, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    audioPlayers[vc.guild.id]!!.playTrack(track)
                }
                override fun playlistLoaded(playlist: AudioPlaylist?) {}
                override fun noMatches() {}
                override fun loadFailed(exception: FriendlyException?) {}
            })
        }

        const val SOUND_BABABOOEY = "https://youtu.be/U_cPir6MwLM"
        const val SOUND_BRUH = "https://youtu.be/2ZIpFytCSVc"
        const val SOUND_HOPELIGHT = "./hopelight.ogg"
        const val SOUND_MORNING = "https://youtu.be/7T_YtklLyyo"
        const val SOUND_OTTER = "https://www.youtube.com/watch?v=H7FbKnzKz4U&ab_channel=JarredBTwin2"
        const val SOUND_VIBING20S = "https://www.youtube.com/watch?v=M94eN-YLbOs&ab_channel=EvanKing"
    }
}