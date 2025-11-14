package com.trading.automated.nb.AutoTrader.services;

import com.trading.automated.nb.AutoTrader.entity.EntryEntity;
import com.trading.automated.nb.AutoTrader.entity.ExitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignalParserServiceTest {

    private SignalParserService signalParserService;

    @BeforeEach
    void setUp() {
        signalParserService = new SignalParserService();
    }

    @Test
    void testGetExitEntity_SingleLeg_100PercentProfit() {
        String message = "SQUARE OFF Modify stop loss and book 100% profit. Sell 25400 CE @ 4";
        ExitEntity[] exitEntity = signalParserService.getExitParams(message);
        ExitEntity exit = exitEntity[0];
        assertNotNull(exit);
        assertEquals("SELL", exit.getAction());
        assertEquals("25400", exit.getStrike());
        assertEquals("CE", exit.getOptionType());
        assertEquals(4.0, exit.getExitPrice());
        assertFalse(exit.isPartialExit());
    }

    @Test
    void testGetExitEntity_SingleLeg_50PercentProfit() {
        String message = "SQUARE OFF Modify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Sell 25400 CE @ 4";
        ExitEntity[] exitEntity = signalParserService.getExitParams(message);
        ExitEntity exit = exitEntity[0];
        assertNotNull(exit);
        assertEquals("SELL", exit.getAction());
        assertEquals("25400", exit.getStrike());
        assertEquals("CE", exit.getOptionType());
        assertEquals(4.0, exit.getExitPrice());
        assertTrue(exit.isPartialExit());
    }

    @Test
    void testGetExitEntity_SingleLeg_TrailingStopLossTriggered() {
        String message = "SQUARE OFF Trailing stop loss triggered. Square off position. Sell 25400 CE @ 4";
        ExitEntity[] exitEntity = signalParserService.getExitParams(message);
        ExitEntity exit = exitEntity[0];
        assertNotNull(exit);

        assertEquals("SELL", exit.getAction());
        assertEquals("25400", exit.getStrike());
        assertEquals("CE", exit.getOptionType());
        assertEquals(4.0, exit.getExitPrice());
        assertFalse(exit.isPartialExit());
    }

    @Test
    void testGetExitEntity_SingleLeg_StopLossTriggered() {
        String message = "SQUARE OFF Stop loss triggered. Modify your stop loss and square off position. Sell 25400 CE @ 4";
        ExitEntity[] exitEntity = signalParserService.getExitParams(message);
        ExitEntity exit = exitEntity[0];
        assertNotNull(exit);
        assertEquals("SELL", exit.getAction());
        assertEquals("25400", exit.getStrike());
        assertEquals("CE", exit.getOptionType());
        assertEquals(4.0, exit.getExitPrice());
        assertFalse(exit.isPartialExit());
    }

    @Test
    void testGetExitEntities_DualLeg_100PercentProfit() {
        String message = "SQUARE OFF Modify stop loss and book 100% profit. Sell 25400 CE @ 12 and Buy 25400 PE @ 23";
        ExitEntity[] exits = signalParserService.getExitParams(message);

        assertNotNull(exits);
        assertEquals(2, exits.length);

        // First leg
        assertEquals("SELL", exits[0].getAction());
        assertEquals("25400", exits[0].getStrike());
        assertEquals("CE", exits[0].getOptionType());
        assertEquals(12.0, exits[0].getExitPrice());
        assertFalse(exits[0].isPartialExit());

        // Second leg
        assertEquals("BUY", exits[1].getAction());
        assertEquals("25400", exits[1].getStrike());
        assertEquals("PE", exits[1].getOptionType());
        assertEquals(23.0, exits[1].getExitPrice());
        assertFalse(exits[1].isPartialExit());
    }

    @Test
    void testGetExitEntities_DualLeg_50PercentProfit() {
        String message = "SQUARE OFF Modify stop loss and book 50% profit and now keep trailing stop loss at cost for remaining 50% qty. Sell 25400 PE @ 23 and Buy 25400 CE @ 12";
        ExitEntity[] exits = signalParserService.getExitParams(message);

        assertNotNull(exits);
        assertEquals(2, exits.length);

        // First leg
        assertEquals("SELL", exits[0].getAction());
        assertEquals("25400", exits[0].getStrike());
        assertEquals("PE", exits[0].getOptionType());
        assertEquals(23.0, exits[0].getExitPrice());
        assertTrue(exits[0].isPartialExit());

        // Second leg
        assertEquals("BUY", exits[1].getAction());
        assertEquals("25400", exits[1].getStrike());
        assertEquals("CE", exits[1].getOptionType());
        assertEquals(12.0, exits[1].getExitPrice());
        assertTrue(exits[1].isPartialExit());
    }

    @Test
    void testGetExitEntities_DualLeg_TrailingStopLossTriggered() {
        String message = "SQUARE OFF Trailing stop loss triggered. Modify stop loss and book profit for remaining 50% quantity. Sell 25400 CE @ 12 and Buy 25400 PE @ 23";
        ExitEntity[] exits = signalParserService.getExitParams(message);

        assertNotNull(exits);
        assertEquals(2, exits.length);

        // First leg
        assertEquals("SELL", exits[0].getAction());
        assertEquals("25400", exits[0].getStrike());
        assertEquals("CE", exits[0].getOptionType());
        assertEquals(12.0, exits[0].getExitPrice());
        assertTrue(exits[0].isPartialExit());

        // Second leg
        assertEquals("BUY", exits[1].getAction());
        assertEquals("25400", exits[1].getStrike());
        assertEquals("PE", exits[1].getOptionType());
        assertEquals(23.0, exits[1].getExitPrice());
        assertTrue(exits[1].isPartialExit());
    }

    @Test
    void testGetExitEntities_DualLeg_StopLossTriggered() {
        String message = "SQUARE OFF Stop loss triggered. Modify your stop loss and square off position. Sell 25400 PE @ 23 and Buy 25400 CE @ 12";
        ExitEntity[] exits = signalParserService.getExitParams(message);

        assertNotNull(exits);
        assertEquals(2, exits.length);

        // First leg
        assertEquals("SELL", exits[0].getAction());
        assertEquals("25400", exits[0].getStrike());
        assertEquals("PE", exits[0].getOptionType());
        assertEquals(23.0, exits[0].getExitPrice());
        assertFalse(exits[0].isPartialExit());

        // Second leg
        assertEquals("BUY", exits[1].getAction());
        assertEquals("25400", exits[1].getStrike());
        assertEquals("CE", exits[1].getOptionType());
        assertEquals(12.0, exits[1].getExitPrice());
        assertFalse(exits[1].isPartialExit());
    }

    @Test
    void testGetEntryParams_SingleLeg_BuyCE() {
        String message = "FRESH TRADE \n\n \"BUY\" 18 Nov \"Nifty 24000 CE\" between 1 - 6\nStop loss for 24000 CE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(1, entries.length);

        EntryEntity entry = entries[0];
        entry.getPriceA();
        assertEquals("BUY", entry.getAction());
        assertEquals("24000", entry.getStrike());
        assertEquals("CE", entry.getOptionType());
        assertEquals(1.0, entry.getStopLoss());
        assertEquals(1.0, entry.getPriceA());
        assertEquals(6.0, entry.getPriceB());
    }

    @Test
    void testGetEntryParams_SingleLeg_BuyPE() {
        String message = "FRESH TRADE \n\n\"BUY\" 18 Nov \"Nifty 24000 PE\" between 1 - 6\nStop loss for 24000 PE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(1, entries.length);

        EntryEntity entry = entries[0];
        assertEquals("BUY", entry.getAction());
        assertEquals("24000", entry.getStrike());
        assertEquals("PE", entry.getOptionType());
        assertEquals(1.0, entry.getStopLoss());
        assertEquals(1.0, entry.getPriceA());
        assertEquals(6.0, entry.getPriceB());
    }

    @Test
    void testGetEntryParams_SingleLeg_SellPE() {
        String message = "FRESH TRADE \n \"SELL\" 18 Nov \"Nifty 24000 PE\" between 2 - 7\nStop loss for 24000 PE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(1, entries.length);

        EntryEntity entry = entries[0];
        assertEquals("SELL", entry.getAction());
        assertEquals("24000", entry.getStrike());
        assertEquals("PE", entry.getOptionType());
        assertEquals(1.0, entry.getStopLoss());
        assertEquals(2.0, entry.getPriceA());
        assertEquals(7.0, entry.getPriceB());
    }

    @Test
    void testGetEntryParams_SingleLeg_SellCE() {
        String message = "FRESH TRADE \"SELL\" 18 Nov \"Nifty 24000 CE\" between 2 - 7\nStop loss for 24000 CE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(1, entries.length);

        EntryEntity entry = entries[0];
        assertEquals("SELL", entry.getAction());
        assertEquals("24000", entry.getStrike());
        assertEquals("CE", entry.getOptionType());
        assertEquals(1.0, entry.getStopLoss());
        assertEquals(2.0, entry.getPriceA());
        assertEquals(7.0, entry.getPriceB());
    }

    @Test
    void testGetEntryParams_DualLeg_BuyCE_SellPE() {
        String message = "FRESH TRADE\n\"BUY\" 18 Nov \"Nifty 24000 CE\" between 1 - 6 \nAND\n\"SELL\" 18 Nov \"Nifty 24000 PE\" between 2 - 7\nStop loss for 24000 CE is 1 and 24000 PE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(2, entries.length);

        // First leg
        assertEquals("BUY", entries[0].getAction());
        assertEquals("24000", entries[0].getStrike());
        assertEquals("CE", entries[0].getOptionType());
        assertEquals(1.0, entries[0].getStopLoss());
        assertEquals(1.0, entries[0].getPriceA());
        assertEquals(6.0, entries[0].getPriceB());

        // Second leg
        assertEquals("SELL", entries[1].getAction());
        assertEquals("24000", entries[1].getStrike());
        assertEquals("PE", entries[1].getOptionType());
        assertEquals(1.0, entries[1].getStopLoss());
        assertEquals(2.0, entries[1].getPriceA());
        assertEquals(7.0, entries[1].getPriceB());
    }

    @Test
    void testGetEntryParams_DualLeg_BuyPE_SellCE() {
        String message = "FRESH TRADE \"BUY\" 18 Nov \"Nifty 24000 PE\" between 1 - 6 AND \"SELL\" 18 Nov \"Nifty 24000 CE\" between 2 - 7\nStop loss for 24000 PE is 1 and 24000 CE is 1";
        EntryEntity[] entries = signalParserService.getEntryParams(message);

        assertNotNull(entries);
        assertEquals(2, entries.length);

        // First leg
        assertEquals("BUY", entries[0].getAction());
        assertEquals("24000", entries[0].getStrike());
        assertEquals("PE", entries[0].getOptionType());
        assertEquals(1.0, entries[0].getStopLoss());
        assertEquals(1.0, entries[0].getPriceA());
        assertEquals(6.0, entries[0].getPriceB());

        // Second leg
        assertEquals("SELL", entries[1].getAction());
        assertEquals("24000", entries[1].getStrike());
        assertEquals("CE", entries[1].getOptionType());
        assertEquals(1.0, entries[1].getStopLoss());
        assertEquals(2.0, entries[1].getPriceA());
        assertEquals(7.0, entries[1].getPriceB());
    }

}