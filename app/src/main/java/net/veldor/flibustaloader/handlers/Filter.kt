package net.veldor.flibustaloader.handlers

import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHOR
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHORS
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_GENRE
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_SEQUENCE
import net.veldor.flibustaloader.selections.BlacklistItem
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.*

object Filter {
    fun check(foundedEntity: FoundedEntity): Boolean {
        if (PreferencesHandler.instance.isUseFilter) {
            var list: ArrayList<BlacklistItem>
            var lowerName: String
            if (foundedEntity.type == TYPE_BOOK) {
                // check for all of blacklists
                if (PreferencesHandler.instance.isOnlyRussian && !foundedEntity.language.contains("ru")) {
                    return false
                }
                list = BlacklistBooks.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.name.isNullOrEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookNameStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.author.isNullOrEmpty()) {
                    lowerName = foundedEntity.author!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookAuthorStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
                list = BlacklistGenres.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.genreComplex.isNullOrEmpty()) {
                    lowerName = foundedEntity.genreComplex!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookGenreStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty() && foundedEntity.sequencesComplex.isNotEmpty()) {
                    lowerName = foundedEntity.sequencesComplex.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookSequenceStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_GENRE) {
                list = BlacklistGenres.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.genreStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_SEQUENCE) {
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.sequenceStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHOR) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.authorStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHORS) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.authorStrictFilter) {
                            if (lowerName == it.name.lowercase()) {
                                return false
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return false
                            }
                        }
                    }
                }
            }
        }
        if (PreferencesHandler.instance.isHideRead) {
            if (foundedEntity.read) {
                return false
            }
        }
        if (PreferencesHandler.instance.isHideDownloaded) {
            if (foundedEntity.downloaded) {
                return false
            }
        }
        if (PreferencesHandler.instance.isHideDigests) {
            if (foundedEntity.authors.size > 2) {
                return false
            }
        }
        return true
    }
}