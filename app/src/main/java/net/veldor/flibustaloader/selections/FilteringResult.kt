package net.veldor.flibustaloader.selections

import android.util.Log

class FilteringResult(val result: Boolean,val value: String?, val rule: String?, val type: String?){
    override fun toString(): String {
        val sb = StringBuffer()
        when(type){
            "hideDigests" -> sb.append("Скрываю сборник, авторы: $value")
            "hideDownloaded" -> sb.append("Скачано ранее")
            "hideReaded" -> sb.append("Прочитано")
            "hideNonRussian" -> sb.append("На иностранном языке")
            "authors strict" -> sb.append("Автор \"$value\" по строгому соответствию: \"$rule\"")
            "authors" -> sb.append("Автор \"$value\" по шаблону: \"$rule\"")
            "book author" -> sb.append("Автор книги \"$value\" по шаблону: \"$rule\"")
            "book author strict" -> sb.append("Автор книги \"$value\" по строгому соответствию: \"$rule\"")
            "sequence strict" -> sb.append("Серия \"$value\" по строгому соответствию: \"$rule\"")
            "sequence" -> sb.append("Серия \"$value\" по шаблону: \"$rule\"")
            "book sequence" -> sb.append("Серия книги \"$value\" по шаблону: \"$rule\"")
            "book sequence strict" -> sb.append("Серия книги \"$value\" по строгому соответствию: \"$rule\"")
            "genre strict" -> sb.append("Жанр \"$value\" по строгому соответствию: \"$rule\"")
            "genre" -> sb.append("Жанр \"$value\" по шаблону: \"$rule\"")
            "book genre" -> sb.append("Жанр книги \"$value\" по шаблону: \"$rule\"")
            "book genre strict" -> sb.append("Жанр книги \"$value\" по строгому соответствию: \"$rule\"")
            "book name" -> sb.append("Книга по названию: \"$value\" по шаблону: \"$rule\"")
            "book name strict" -> sb.append("Книга по названию: \"$value\" по строгому соответствию: \"$rule\"")
            "format" -> sb.append("Книга по формату: \"$value\" по шаблону: \"$rule\"")
        }
        return sb.toString()
    }
}