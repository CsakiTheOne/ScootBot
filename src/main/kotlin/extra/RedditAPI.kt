package extra

import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Account
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import java.io.File

class RedditAPI {
    companion object {
        private var reddit: RedditClient? = null

        fun login() {
            val username = Secret.getRedditUsername()
            val password = Secret.getRedditPassword()

            val oauthCreds: Credentials = Credentials.script(username, password, "gIw9wV7dlenQ2SrrD16sQQ", "ISC3XdDy0u953CgxG6DIaTS0-cJLrg")
            val userAgent = UserAgent("bot", "com.csakitheone.booba", "1.0.0", "CsakiTheOne")
            reddit = OAuthHelper.automatic(OkHttpNetworkAdapter(userAgent), oauthCreds)

            val me: Account? = reddit!!.me().query().account
            println("<RedditAPI> Logged in! User info: $me")
        }

        fun getRandomPost(subredditName: String): String {
            if (reddit == null) return ""

            return reddit!!.subreddit(subredditName).randomSubmission().subject.url
        }

        /*
        (AnaFoxxx)
        (DarshelleStevens)
        FuckingPerfect
        GoneWildSmiles
        PornDio
        rule34
        SexyTummies
        SoGoodToBeBad
        starwarsnsfw
        TeenTitansPorn
        yummypawgs
         */
    }
}