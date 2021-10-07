package net.veldor.flibustaloader.utils

class SubscribeSequences private constructor(): SubscribeType(){
    override val subscribeName = "sequence"

    companion object {
        @JvmStatic
        var instance: SubscribeSequences = SubscribeSequences()
            private set
    }

    init {
        subscribeFileName = MyFileReader.SEQUENCES_SUBSCRIBE_FILE
    }
}