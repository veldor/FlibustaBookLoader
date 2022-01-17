package net.veldor.flibustaloader.utils

class BlacklistFormat private constructor() : BlacklistType(){

    override val blacklistName = "format"

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