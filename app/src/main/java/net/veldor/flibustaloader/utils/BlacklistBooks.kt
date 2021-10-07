package net.veldor.flibustaloader.utils

class BlacklistBooks private constructor() : BlacklistType(){
    override val blacklistName = "book"

    companion object {
        @JvmStatic
        var instance: BlacklistBooks = BlacklistBooks()
            private set
    }
    init {
        blacklistFileName = MyFileReader.BOOKS_BLACKLIST_FILE
        refreshBlacklist()
    }
}