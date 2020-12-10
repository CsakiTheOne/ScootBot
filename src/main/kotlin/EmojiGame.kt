class EmojiGame {
    companion object {
        val hats = listOf(" ", "💩", "👒", "🎩", "🎓")
        val heads = listOf(
            "😃", "😁", "😂", "😄", "😅", "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗", "☺", "🤗", "🤩", "🤔", "🤨", "😐",
            "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "😴", "😌", "😛", "😜", "😝", "🤤", "😒", "😓", "😕", "🙃", "😢", "😭",
            "🤯", "😬", "🥵", "🥶", "😳", "🤪", "😵", "🥴", "😠", "🤬", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "😇", "🥳",
            "🥺", "🤠", "🤡", "🤫", "🤭", "🧐", "🤓", "👽", "🤖"
        )
        val bodies = listOf("👕", "👗", "👘", "👚", "🥋", "🥻")
        val bottoms = listOf("👖")
        val shoes = listOf("👞👞", "👟👟", "🧦", "👢👢", "🦶🦶", "🦵🦵")

        fun generate() : String {
            return "${hats.random()}\n${heads.random()}\n${bodies.random()}\n${bottoms.random()}\n${shoes.random()}"
        }
    }
}