package com.hacklab.minecraft.skills.party

import java.util.UUID
import java.util.LinkedHashSet

data class Party(
    val id: UUID = UUID.randomUUID(),
    var leaderId: UUID,
    val members: LinkedHashSet<UUID> = linkedSetOf(),
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        if (members.isEmpty()) {
            members.add(leaderId)
        }
    }

    fun isFull(maxSize: Int): Boolean = members.size >= maxSize
    fun isLeader(playerId: UUID): Boolean = leaderId == playerId
    fun isMember(playerId: UUID): Boolean = members.contains(playerId)
    fun memberCount(): Int = members.size
}
