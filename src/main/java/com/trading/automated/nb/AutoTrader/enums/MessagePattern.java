package com.trading.automated.nb.AutoTrader.enums;

public enum MessagePattern {
    // Signals a new trade entry (e.g., "BUY 26 OCT NIFTY 23500 CE between 100 - 110")
    ENTRY_SIGNAL,
    // Any other message, or a message that doesn't match the required formats
    UNKNOWN_SIGNAL,
    EXIT_SQUARE_OFF_SIGNAL, // General square-off signal
    EXIT_SQUARE_OFF_DUAL_LEG, // Dual-leg square-off
    EXIT_SQUARE_OFF_SINGLE_LEG; // Single-leg square-off
}