package net.veldor.flibustaloader.handlers

import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.*

object Filter {
    fun check(foundedEntity: FoundedEntity): Boolean {
        if (PreferencesHandler.instance.isUseFilter) {
            // check for all of blacklists
                if(PreferencesHandler.instance.isOnlyRussian && !foundedEntity.language.contains("ru")){
                    return false
                }
            var list = BlacklistBooks.instance.getBlacklist()
            if (list.isNotEmpty() && !foundedEntity.name.isNullOrEmpty()) {
                val lowerName = foundedEntity.name!!.lowercase()
                list.forEach {
                    if (lowerName.contains(it.name)) {
                        return false
                    }
                }
            }
            list = BlacklistAuthors.instance.getBlacklist()
            if (list.isNotEmpty() && !foundedEntity.author.isNullOrEmpty()) {
                val lowerName = foundedEntity.author!!.lowercase()
                list.forEach {
                    if (lowerName.contains(it.name)) {
                        return false
                    }
                }
            }
            list = BlacklistGenres.instance.getBlacklist()
            if (list.isNotEmpty() && !foundedEntity.genreComplex.isNullOrEmpty()) {
                val lowerName = foundedEntity.genreComplex!!.lowercase()
                list.forEach {
                    if (lowerName.contains(it.name)) {
                        return false
                    }
                }
            }
            list = BlacklistSequences.instance.getBlacklist()
            if (list.isNotEmpty() && foundedEntity.sequencesComplex.isNotEmpty()) {
                val lowerName = foundedEntity.sequencesComplex.lowercase()
                list.forEach {
                    if (lowerName.contains(it.name)) {
                        return false
                    }
                }
            }
        }
        if(PreferencesHandler.instance.isHideRead){
            if(foundedEntity.read){
                return false
            }
        }
        if(PreferencesHandler.instance.isHideDownloaded){
            if(foundedEntity.downloaded){
                return false
            }
        }
        if(PreferencesHandler.instance.isHideDigests){
            if(foundedEntity.authors.size > 2){
                return false
            }
        }
        return true
    }
}