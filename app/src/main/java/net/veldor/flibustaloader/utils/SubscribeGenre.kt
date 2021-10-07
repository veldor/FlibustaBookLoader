package net.veldor.flibustaloader.utils

class SubscribeGenre private constructor(): SubscribeType(){
    override val subscribeName = "genre"

    companion object {
        @JvmStatic
        var instance: SubscribeGenre = SubscribeGenre()
            private set
    }


    init {
        subscribeFileName = MyFileReader.GENRES_SUBSCRIBE_FILE
    }
}