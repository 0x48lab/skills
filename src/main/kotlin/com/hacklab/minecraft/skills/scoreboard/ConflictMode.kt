package com.hacklab.minecraft.skills.scoreboard

/**
 * Conflict mode for scoreboard handling when other plugins use SIDEBAR.
 */
enum class ConflictMode {
    /**
     * Always show Skills scoreboard, overwriting other plugins.
     */
    ALWAYS,

    /**
     * Respect other plugins - don't show if SIDEBAR is already in use by another plugin.
     */
    RESPECT;

    companion object {
        /**
         * Parse a string to ConflictMode, defaulting to RESPECT for invalid values.
         */
        fun fromString(value: String): ConflictMode {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                RESPECT // Safe default - respect other plugins
            }
        }
    }
}
