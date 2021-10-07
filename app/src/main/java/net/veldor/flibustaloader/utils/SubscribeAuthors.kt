package net.veldor.flibustaloader.utils

class SubscribeAuthors private constructor(): SubscribeType(){
    override val subscribeName = "author"

    companion object {
        @JvmStatic
        var instance: SubscribeAuthors = SubscribeAuthors()
            private set
    }


    init {
        subscribeFileName = MyFileReader.AUTHORS_SUBSCRIBE_FILE
    }
}