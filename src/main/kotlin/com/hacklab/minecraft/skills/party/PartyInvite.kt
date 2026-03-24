package com.hacklab.minecraft.skills.party

import java.util.UUID

data class PartyInvite(
    val inviterId: UUID,
    val inviteeId: UUID,
    val partyId: UUID,
    val expiresAt: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}
