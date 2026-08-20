package com.skyblockexp.ezeconomy.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.skyblockexp.ezeconomy.api.storage.StorageProvider;
import com.skyblockexp.ezeconomy.api.storage.models.Transaction;
import com.skyblockexp.ezeconomy.storage.TransferResult;
import com.skyblockexp.ezeconomy.service.PlayerEconomyService;
import com.skyblockexp.ezeconomy.service.CurrencyService;
import com.skyblockexp.ezeconomy.dto.PlayerBalanceDTO;

/**
 * EzEconomyAPI v2 - Standalone, not Minecraft/Bukkit dependent.
 * <p>
 * Provides a complete, versioned API for player economy operations, supporting multi-currency and transaction history.
 * All operations use UUIDs and currency codes for maximum compatibility and modularity.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 *     EzEconomyAPI api = new EzEconomyAPI(storageProvider);
 *     double balance = api.getBalance(playerUuid, "dollar");
 *     api.deposit(playerUuid, "euro", 100.0);
 * </pre>
 * </p>
 * @author EzEconomy
 * @version 2.0.0
 */
public class EzEconomyAPI {
    /** API version string. */
    public static final String VERSION = "2.0.0";

    private final PlayerEconomyService playerService;
    private final CurrencyService currencyService;

    /**
     * Construct a new EzEconomyAPI instance.
     * @param storageProvider The storage provider backend to use.
     */
    public EzEconomyAPI(StorageProvider storageProvider) {
        this.playerService = new PlayerEconomyService(storageProvider);
        this.currencyService = new CurrencyService(storageProvider);
    }

    /**
     * Get the API version string.
     * @return API version (e.g., "2.0.0")
     */
    public String getVersion() {
        return VERSION;
    }

    // --- Player Balances & Transactions ---

    /**
     * Get a player's balance for a specific currency.
     * @param uuid Player UUID
     * @param currency Currency code (e.g., "dollar", "euro")
     * @return Player's balance
     */
    public PlayerBalanceDTO getBalance(UUID uuid, String currency) {
        double balance = playerService.getBalance(uuid, currency);
        return new PlayerBalanceDTO(uuid, currency, balance);
    }

    /**
     * Deposit an amount to a player's balance for a specific currency.
     * @param uuid Player UUID
     * @param currency Currency code
     * @param amount Amount to deposit
     * @return true if successful
     */
    public boolean deposit(UUID uuid, String currency, double amount) {
        return playerService.deposit(uuid, currency, amount);
    }

    /**
     * Withdraw an amount from a player's balance for a specific currency.
     * @param uuid Player UUID
     * @param currency Currency code
     * @param amount Amount to withdraw
     * @return true if successful, false if insufficient funds
     */
    public boolean withdraw(UUID uuid, String currency, double amount) {
        return playerService.withdraw(uuid, currency, amount);
    }

    /**
     * Get a player's transaction history for a specific currency.
     * @param uuid Player UUID
     * @param currency Currency code
     * @return List of transactions
     */
    public List<Transaction> getTransactions(UUID uuid, String currency) {
        return playerService.getTransactions(uuid, currency);
    }

    /**
     * Get all player balances for a specific currency.
     * @param currency Currency code
     * @return Map of UUID to balance
     */
    public Map<UUID, Double> getAllBalances(String currency) {
        return playerService.getAllBalances(currency);
    }

    /**
     * Transfer an amount from one player to another for a specific currency.
     * @param fromUuid Sender UUID
     * @param toUuid Recipient UUID
     * @param currency Currency code
     * @param amount Amount to transfer
     * @return TransferResult with updated balances and status
     */
    public TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double amount) {
        return playerService.transfer(fromUuid, toUuid, currency, amount);
    }

    /**
     * Transfer custom debit and credit amounts between two players for a specific currency.
     * @param fromUuid Sender UUID
     * @param toUuid Recipient UUID
     * @param currency Currency code
     * @param debitAmount Amount to withdraw from sender
     * @param creditAmount Amount to deposit to recipient
     * @return TransferResult with updated balances and status
     */
    public TransferResult transfer(UUID fromUuid, UUID toUuid, String currency, double debitAmount, double creditAmount) {
        return playerService.transfer(fromUuid, toUuid, currency, debitAmount, creditAmount);
    }

    // --- Multi-Currency Support ---

    /**
     * Get the default currency code (e.g., "dollar").
     * @return Default currency code
     */
    public String getDefaultCurrency() {
        return currencyService.getDefaultCurrency();
    }

    /**
     * Get all available currency codes (e.g., ["dollar", "euro", "gem"]).
     * @return Set of currency codes
     */
    public Set<String> getAvailableCurrencies() {
        return currencyService.getAvailableCurrencies();
    }

    /**
     * Check if a currency is enabled in the configuration.
     * @param currency Currency code
     * @return true if enabled, false otherwise
     */
    public boolean isCurrencyEnabled(String currency) {
        return currencyService.isCurrencyEnabled(currency);
    }
}
