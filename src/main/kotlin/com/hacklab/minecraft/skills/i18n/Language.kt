package com.hacklab.minecraft.skills.i18n

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語");

    companion object {
        fun fromCode(code: String): Language =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH

        fun fromLocale(locale: String): Language {
            return when {
                locale.startsWith("ja") -> JAPANESE
                else -> ENGLISH
            }
        }
    }
}
