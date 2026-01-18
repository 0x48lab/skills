package com.hacklab.minecraft.skills.nms

import org.bukkit.Bukkit

/**
 * Represents the detected NMS version.
 * Used to handle version-specific reflection.
 */
data class NmsVersion(
    val major: Int,      // e.g., 1
    val minor: Int,      // e.g., 21
    val patch: Int,      // e.g., 4
    val revision: String // e.g., "R1", "R2", "R3"
) {
    /**
     * Check if this version is at least the specified version.
     */
    fun isAtLeast(major: Int, minor: Int, patch: Int = 0): Boolean {
        if (this.major > major) return true
        if (this.major < major) return false
        if (this.minor > minor) return true
        if (this.minor < minor) return false
        return this.patch >= patch
    }

    /**
     * Check if this version matches the major.minor version.
     */
    fun matches(major: Int, minor: Int): Boolean {
        return this.major == major && this.minor == minor
    }

    /**
     * Get the CraftBukkit package path.
     * Paper 1.20.5+ uses unversioned packages.
     */
    fun getCraftBukkitPackage(): String {
        return if (isAtLeast(1, 20, 5)) {
            "org.bukkit.craftbukkit"
        } else {
            "org.bukkit.craftbukkit.v${major}_${minor}_$revision"
        }
    }

    override fun toString(): String = "$major.$minor.$patch-$revision"

    companion object {
        private var cached: NmsVersion? = null

        /**
         * Detect the current server NMS version.
         */
        fun detect(): NmsVersion {
            cached?.let { return it }

            val bukkitVersion = Bukkit.getBukkitVersion() // e.g., "1.21.4-R0.1-SNAPSHOT"
            val serverVersion = Bukkit.getVersion() // e.g., "git-Paper-123 (MC: 1.21.4)"

            // Parse version from bukkit version string
            val versionRegex = Regex("""(\d+)\.(\d+)(?:\.(\d+))?-R(\d+)""")
            val match = versionRegex.find(bukkitVersion)

            val version = if (match != null) {
                NmsVersion(
                    major = match.groupValues[1].toInt(),
                    minor = match.groupValues[2].toInt(),
                    patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0,
                    revision = "R${match.groupValues[4]}"
                )
            } else {
                // Fallback: try to parse from server version
                val mcVersionRegex = Regex("""MC:\s*(\d+)\.(\d+)(?:\.(\d+))?""")
                val mcMatch = mcVersionRegex.find(serverVersion)
                if (mcMatch != null) {
                    NmsVersion(
                        major = mcMatch.groupValues[1].toInt(),
                        minor = mcMatch.groupValues[2].toInt(),
                        patch = mcMatch.groupValues.getOrNull(3)?.toIntOrNull() ?: 0,
                        revision = "R1"
                    )
                } else {
                    // Default fallback
                    NmsVersion(1, 21, 0, "R1")
                }
            }

            cached = version
            return version
        }

        /**
         * Clear cached version (for testing).
         */
        fun clearCache() {
            cached = null
        }
    }
}
