package org.example.formulaone.util;

import java.math.BigDecimal;

/**
 * Simple constants for the betting system.
 */
public class Constants {
    public static final BigDecimal GIFT_BALANCE = new BigDecimal("100.00");
    public static final String BET_STATUS_PENDING = "PENDING";
    public static final String BET_STATUS_FAILED = "FAILED";
    public static final String BET_STATUS_WON = "WON";
    public static final String BET_STATUS_LOST = "LOST";
    public static final String ERROR_INSUFFICIENT_BALANCE = "insufficient_balance";
    public static final String SUCCESS_BET_PLACED = "Bet placed successfully";
}
