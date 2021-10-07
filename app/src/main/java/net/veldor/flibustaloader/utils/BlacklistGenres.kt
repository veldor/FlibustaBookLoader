package net.veldor.flibustaloader.utils

class BlacklistGenres private constructor() : BlacklistType(){

    override val blacklistName = "genre"

    companion object {
        @JvmStatic
        var instance: BlacklistGenres = BlacklistGenres()
            private set
    }
    init {
        blacklistFileName = MyFileReader.GENRES_BLACKLIST_FILE
        refreshBlacklist()
    }
}