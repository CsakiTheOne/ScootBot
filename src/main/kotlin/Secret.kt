class Secret {
    companion object {
        private val token = "NzgzNjcyMjU3MzQ3NzE1MTIz.X8eJqg.pHW9ENgBWidFnVDej42yZNrUAP4"

        fun getToken(reason: String) : String {
            println("Token given. Reason: $reason")
            return token
        }
    }
}