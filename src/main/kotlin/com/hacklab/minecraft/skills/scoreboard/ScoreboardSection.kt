package com.hacklab.minecraft.skills.scoreboard

enum class ScoreboardSection(val bit: Int, val label: String) {
    TITLE(0x01, "title"),
    HMS(0x02, "hms"),
    GOLD(0x04, "gold"),
    STATS(0x08, "stats"),
    PARTY(0x10, "party");

    companion object {
        fun fromLabel(label: String): ScoreboardSection? =
            entries.find { it.label.equals(label, ignoreCase = true) }

        /** All sections enabled */
        const val ALL = 0x1F
    }
}
