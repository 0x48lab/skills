package com.hacklab.minecraft.skills.magic

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Interface for ongoing spell effects that drain mana over time.
 * Each implementation handles its own logic (mana drain, ActionBar, effects).
 * The ActiveSpellManager manages the lifecycle and tick scheduling.
 */
interface OngoingSpellEffect {
    val playerId: UUID
    val spellType: SpellType

    /** Called when the effect starts. Player is guaranteed to be online. */
    fun onStart(player: Player)

    /** Called every second (20 ticks). Return false to end the effect. */
    fun tick(player: Player): Boolean

    /** Called when the effect ends for any reason. */
    fun onEnd(player: Player, reason: EndReason)
}

enum class EndReason {
    /** Mana depleted or time expired */
    EXPIRED,
    /** Player manually cancelled (/cast cancel) */
    CANCELLED,
    /** Dispelled by another player */
    DISPELLED,
    /** Player died */
    DEATH,
    /** Player disconnected */
    DISCONNECT,
    /** World change restriction (e.g., entering The End) */
    WORLD_CHANGE,
    /** Environmental factor (e.g., entering water) */
    ENVIRONMENTAL
}
