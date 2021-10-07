package net.veldor.flibustaloader.utils

class BlacklistAuthors private constructor() : BlacklistType(){
    override val blacklistName = "author"

    companion object {

        @JvmStatic
        var instance: BlacklistAuthors = BlacklistAuthors()
            private set
    }
    init {
        blacklistFileName = MyFileReader.AUTHORS_BLACKLIST_FILE
        refreshBlacklist()
    }
}