package net.veldor.flibustaloader.utils

class BlacklistGenre private constructor() : BlacklistType(){

    override val blacklistName = "genre"

    companion object {
        @JvmStatic
        var instance: BlacklistGenre = BlacklistGenre()
            private set
    }
    init {
        blacklistFileName = MyFileReader.GENRES_BLACKLIST_FILE
        refreshBlacklist()
    }
}