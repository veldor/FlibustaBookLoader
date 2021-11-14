package net.veldor.flibustaloader.handlers

import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHOR
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_AUTHORS
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_GENRE
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_SEQUENCE
import net.veldor.flibustaloader.selections.BlacklistItem
import net.veldor.flibustaloader.selections.FilteringResult
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.*

object Filter {
    fun check(foundedEntity: FoundedEntity): FilteringResult {
        if (PreferencesHandler.instance.isUseFilter) {
            var list: ArrayList<BlacklistItem>
            var lowerName: String
            if (foundedEntity.type == TYPE_BOOK) {
                // check for all of blacklists
                if (PreferencesHandler.instance.isOnlyRussian && !foundedEntity.language.contains("ru")) {
                    return FilteringResult(true, null, null, "hideNonRussian")
                }
                list = BlacklistBooks.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.name.isNullOrEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookNameStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book name strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book name"
                                )
                            }
                        }
                    }
                }
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.author.isNullOrEmpty()) {
                    lowerName = foundedEntity.author!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookAuthorStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book author strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book author"
                                )
                            }
                        }
                    }
                }
                list = BlacklistGenres.instance.getBlacklist()
                if (list.isNotEmpty() && !foundedEntity.genreComplex.isNullOrEmpty()) {
                    lowerName = foundedEntity.genreComplex!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookGenreStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book genre strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book genre"
                                )
                            }
                        }
                    }
                }
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty() && foundedEntity.sequencesComplex.isNotEmpty()) {
                    lowerName = foundedEntity.sequencesComplex.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.bookSequenceStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book sequence strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "book sequence"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_GENRE) {
                list = BlacklistGenres.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.genreStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "genre strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "genre"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_SEQUENCE) {
                list = BlacklistSequences.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.sequenceStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "sequence strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "sequence"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHOR) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.authorStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "authors strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "authors"
                                )
                            }
                        }
                    }
                }
            } else if (foundedEntity.type == TYPE_AUTHORS) {
                list = BlacklistAuthors.instance.getBlacklist()
                if (list.isNotEmpty()) {
                    lowerName = foundedEntity.name!!.lowercase()
                    list.forEach {
                        if (PreferencesHandler.instance.authorStrictFilter()) {
                            if (lowerName == it.name.lowercase()) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "authors strict"
                                )
                            }
                        } else {
                            if (lowerName.contains(it.name.lowercase())) {
                                return FilteringResult(
                                    false,
                                    lowerName,
                                    it.name.lowercase(),
                                    "authors"
                                )
                            }
                        }
                    }
                }
            }
        }
        if (PreferencesHandler.instance.isHideRead) {
            if (foundedEntity.read) {
                return FilteringResult(false, null, null, "hideReaded")
            }
        }
        if (PreferencesHandler.instance.isHideDownloaded) {
            if (foundedEntity.downloaded) {
                return FilteringResult(false, null, null, "hideDownloaded")
            }
        }
        if (PreferencesHandler.instance.isHideDigests) {
            if (foundedEntity.authors.size > 2) {
                return FilteringResult(false, foundedEntity.author, null, "hideDigests")
            }
        }
        return FilteringResult(true, null, null, null)
    }
}