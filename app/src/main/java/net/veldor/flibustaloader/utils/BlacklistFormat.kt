package net.veldor.flibustaloader.utils

class BlacklistFormat private constructor() : BlacklistType(){

    override val blacklistName = "genre"

    companion object {
        @JvmStatic
        var instance: BlacklistFormat = BlacklistFormat()
            private set
    }
    init {
        blacklistFileName = MyFileReader.FORMAT_BLACKLIST_FILE
        refreshBlacklist()
    }
}