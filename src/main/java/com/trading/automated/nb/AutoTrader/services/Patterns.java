package com.trading.automated.nb.AutoTrader.services;

import java.util.regex.Pattern;

public class Patterns {

    // --- Patterns for Signal Recognition (FINAL SOLUTION) ---

    // Expected Cleaned Format: BUY-21 OCT-NIFTY 25900 CE-BETWEEN 145-135
    public static final String ENTRY_REGEX = "(?:[A-Z]+\\s*)*"                  // optional prefix words like FRESH TRADE
            + "\"\\s*(BUY)\\s*\""                // "BUY"
            + "\\s*(?:[-–]\\s*)?"                // optional hyphen or en-dash
            + "(\\d+\\s*[A-Z]{2,4}\\s*\\d*)"     // date like 4 Nov or 14Oct or 4 Nov 2025
            + "(?:\\s*[-–]\\s*)?"                // optional hyphen or en-dash before instrument
            + "\"(.+?)\""                        // instrument
            + "\\s*between\\s*"
            + "(\\d+\\.?\\d*)"                   // price start
            + "\\s*[-–]\\s*"                     // hyphen/en-dash between prices
            + "(\\d+\\.?\\d*)";

    public static final Pattern ENTRY_PATTERN = Pattern.compile(ENTRY_REGEX,
            Pattern.CASE_INSENSITIVE); // Case is handled by toUpperCase() in cleanInput

    // PROFIT BOOKING Pattern (Must match the clean, hyphenated output)
    public static final String PROFIT_BOOKING_REGEX =
            // Option 1: Partial Profit/SL Modification (using hyphens)
            "(?:MODIFY[-]STOP[-]LOSS[-]AND[-])?BOOK[-]\\d+%[-]PROFIT|" +
                    // Option 2: Full Profit Booking (e.g., BOOK-FULL-PROFIT)
                    "\\b(BOOK|PROFIT)[-](FULL|PARTIAL|REMAINING|TARGET)\\b";

    public static final Pattern PROFIT_BOOKING_PATTERN = Pattern.compile(PROFIT_BOOKING_REGEX,
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // STOP LOSS TRIGGERED Pattern (Must match the clean, hyphenated output)
    public static final String STOP_LOSS_TRIGGERED_REGEX =
            "STOP[-]LOSS[-]TRIGGERED";

    public static final Pattern STOP_LOSS_TRIGGERED_PATTERN = Pattern.compile(STOP_LOSS_TRIGGERED_REGEX,
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    // -------------------------
    // SQUARE-OFF PATTERN (robust, covers 25 variants)
    // -------------------------
    /*
      Strategy:
       - Match "SQUARE OFF" (word boundary)
       - Allow up to N characters (including newlines) until we find one of the trigger phrases:
         MODIFY STOP LOSS, BOOK X% PROFIT, TRAILING STOP LOSS TRIGGERED, STOP LOSS TRIGGERED, or SQUARE OFF POSITION
       - Then allow up to N chars again and require one or two legs:
         SELL|BUY <strike> CE|PE @ <price> [AND SELL|BUY <strike> CE|PE @ <price>]
       - Using [\\s\\S] instead of DOTALL for reliable multiline matching.
    */
    public static final String SQUARE_OFF_REGEX =
            "(?i)" +                                           // case-insensitive
                    "\\bSQUARE\\s*OFF\\b" +
                    "[\\s\\S]{0,300}?" +                               // up to ~300 chars (safe cushion) including newlines
                    "\\b(?:MODIFY\\s*STOP\\s*LOSS|BOOK\\s*\\d+%\\s*PROFIT|TRAILING\\s*STOP\\s*LOSS\\s*TRIGGERED|STOP\\s*LOSS\\s*TRIGGERED|SQUARE\\s*OFF\\s*POSITION)\\b" +
                    "[\\s\\S]{0,300}?" +
                    // leg1
                    "\\b(SELL|BUY)\\s*\\d{3,6}\\s*(?:CE|PE)\\s*@\\s*\\d{1,4}\\b" +
                    // optional leg2 (AND ... )
                    "(?:[\\s,\\-]*AND[\\s,\\-]*\\b(SELL|BUY)\\s*\\d{3,6}\\s*(?:CE|PE)\\s*@\\s*\\d{1,4}\\b)?";

    public static final Pattern SQUARE_OFF_PATTERN = Pattern.compile(SQUARE_OFF_REGEX,
            Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES);
}
