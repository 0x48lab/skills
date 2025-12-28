package com.hacklab.minecraft.skills.economy

import com.hacklab.minecraft.skills.Skills
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player

/**
 * Vault economy integration hook
 */
class VaultHook(private val plugin: Skills) {

    private var economy: Economy? = null

    /**
     * Setup Vault economy hook
     * @return true if Vault was found and economy is available
     */
    fun setup(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.logger.info("Vault not found - economy features disabled")
            return false
        }

        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            plugin.logger.warning("Vault found but no economy plugin registered")
            return false
        }

        economy = rsp.provider
        plugin.logger.info("Vault economy hooked: ${economy?.name}")
        return true
    }

    /**
     * Check if economy is available
     */
    fun isEnabled(): Boolean = economy != null

    /**
     * Deposit money to a player
     * @return true if successful
     */
    fun deposit(player: Player, amount: Double): Boolean {
        val econ = economy ?: return false
        if (amount <= 0) return false

        val response = econ.depositPlayer(player, amount)
        return response.transactionSuccess()
    }

    /**
     * Withdraw money from a player
     * @return true if successful
     */
    fun withdraw(player: Player, amount: Double): Boolean {
        val econ = economy ?: return false
        if (amount <= 0) return false

        val response = econ.withdrawPlayer(player, amount)
        return response.transactionSuccess()
    }

    /**
     * Get player's balance
     */
    fun getBalance(player: Player): Double {
        val econ = economy ?: return 0.0
        return econ.getBalance(player)
    }

    /**
     * Check if player has enough money
     */
    fun has(player: Player, amount: Double): Boolean {
        val econ = economy ?: return false
        return econ.has(player, amount)
    }

    /**
     * Format currency amount for display
     */
    fun format(amount: Double): String {
        val econ = economy ?: return "%.0f".format(amount)
        return econ.format(amount)
    }

    /**
     * Get currency name (singular)
     */
    fun getCurrencyName(): String {
        return economy?.currencyNameSingular() ?: "coin"
    }

    /**
     * Get currency name (plural)
     */
    fun getCurrencyNamePlural(): String {
        return economy?.currencyNamePlural() ?: "coins"
    }
}
