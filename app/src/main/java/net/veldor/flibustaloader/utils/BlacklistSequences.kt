package net.veldor.flibustaloader.utils

class BlacklistSequences private constructor() : BlacklistType() {
    override val blacklistName = "sequence"

    companion object {

        @JvmStatic
        var instance: BlacklistSequences = BlacklistSequences()
            private set
    }

    init {
        blacklistFileName = MyFileReader.SEQUENCES_BLACKLIST_FILE
        refreshBlacklist()
    }
}